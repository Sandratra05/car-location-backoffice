package com.example.service;

import com.example.config.DbConnection;
import com.example.entity.Reservation;
import com.example.entity.Vehicule;
import com.example.enums.TypeCarburant;
import com.example.repository.VehiculeRepository;

import java.util.Map;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Random;

import com.example.entity.Distance;
import com.example.entity.Hotel;
import com.example.entity.Parametre;
import com.example.entity.Assignation;
import com.example.repository.DistanceRepository;
import com.example.repository.ParametreRepository;

public class VehiculeService {
    
    private final VehiculeRepository vehiculeRepository;

    // Dernières heures de départ calculées (par véhicule) lors d'un `assignByIntervals`.
    // On y met l'heure de départ de l'intervalle (commune à tous les véhicules de l'intervalle).
    private final Map<Vehicule, Timestamp> lastDepartTimes = new HashMap<>();

    public VehiculeService() {
        this.vehiculeRepository = new VehiculeRepository();
    }

    public Vehicule createVehicule(Vehicule vehicule) throws SQLException {
        // Validation
        if (vehicule.getReference() == null || vehicule.getReference().isEmpty()) {
            throw new IllegalArgumentException("La référence est obligatoire");
        }
        if (vehicule.getNbPlace() <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être positif");
        }
        if (vehicule.getTypeCarburant() == null) {
            throw new IllegalArgumentException("Le type de carburant est obligatoire");
        }
        
        return vehiculeRepository.save(vehicule);
    }

    public List<Vehicule> getAllVehicules() throws SQLException {
        return vehiculeRepository.findAll();
    }

    public Vehicule getVehiculeById(Long id) throws SQLException {
        Vehicule vehicule = vehiculeRepository.findById(id);
        if (vehicule == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        return vehicule;
    }

    public Vehicule updateVehicule(Long id, Vehicule vehicule) throws SQLException {
        // Vérifier que le véhicule existe
        Vehicule existing = vehiculeRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        
        vehicule.setId(id);
        return vehiculeRepository.update(vehicule);
    }

    public void deleteVehicule(Long id) throws SQLException {
        Vehicule existing = vehiculeRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        vehiculeRepository.deleteById(id);
    }

    // Fonction encore à modifier parce qu'il n'y a pas encore de table d'association entre reservation et vehicule
    public List<Vehicule> findAvailableVehicules(Timestamp date) throws SQLException {
        List<Vehicule> available = new ArrayList<>();

        String sql = """
            SELECT v.*
            FROM vehicule v
            WHERE v.id NOT IN (
                SELECT rv.vehicule_id FROM reservation_vehicule rv
                JOIN reservation r ON rv.reservation_id = r.id_reservation
                WHERE DATE(r.date_heure_arrivee) = ?
            )
            ORDER BY v.id
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, new Date(date.getTime()));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vehicule v = new Vehicule(
                        rs.getLong("id"),
                        rs.getString("reference"),
                        rs.getInt("nb_place"),
                        TypeCarburant.valueOf(rs.getString("type_carburant"))
                    );
                    available.add(v);
                }
            }
            return available;
        } catch (SQLException e) {
            // If the association table doesn't exist yet, fallback to returning all vehicles
            return vehiculeRepository.findAll();
        }
    }

   

    public Map<Vehicule, List<Reservation>> assignVehiculeToReservation(List<Reservation> reservations) throws SQLException {
        if (reservations == null || reservations.isEmpty()) {
            return new HashMap<>();
        }

        List<Vehicule> allVehicules = getAllVehicules();

        Map<Vehicule, List<Reservation>> assignments = new HashMap<>();
        Map<Vehicule, Integer> remaining = new HashMap<>();
        for (Vehicule v : allVehicules) {
            remaining.put(v, v.getNbPlace());
        }

        // Nouvelle logique : prendre la plus grande réservation non assignée,
        // l'assigner au véhicule le plus adapté (best-fit : plus petite capacité qui peut contenir la resa),
        // puis essayer de remplir ce même véhicule avec les plus grandes réservations restantes qui entrent.
        List<Reservation> unassigned = new ArrayList<>(reservations);
        // Trier décroissant par nombre de passagers 
        unassigned.sort((a, b) -> {
            int na = a.getNbPassager() != null ? a.getNbPassager() : 0;
            int nb = b.getNbPassager() != null ? b.getNbPassager() : 0;
            return Integer.compare(nb, na);
        });

        while (!unassigned.isEmpty()) {
            Reservation current = unassigned.get(0);
            int needed = current.getNbPassager() != null ? current.getNbPassager() : 0;
            if (needed <= 0) {
                unassigned.remove(0);
                continue;
            }

            // Best-fit : choisir le véhicule avec la plus petite capacité totale qui puisse contenir la resa
            Vehicule chosen = null;
            int bestCap = Integer.MAX_VALUE;
            for (Vehicule v : allVehicules) {
                int rem = remaining.getOrDefault(v, v.getNbPlace());
                if (rem >= needed) {
                    int cap = v.getNbPlace();
                    if (cap < bestCap) {
                        bestCap = cap;
                        chosen = v;
                    } else if (cap == bestCap) {
                        // si même capacité, choisir par moins de trajets puis carburant puis aléatoire
                        if (chosen != null) {
                            List<Vehicule> tie = new ArrayList<>();
                            tie.add(chosen);
                            tie.add(v);
                            chosen = chooseByTrajetsThenCarburant(tie);
                        } else {
                            chosen = v;
                        }
                    }
                }
            }

            if (chosen == null) {
                // Aucune voiture ne peut contenir cette réservation -> la laisser non assignée et la retirer
                unassigned.remove(0);
                continue;
            }

            // Assigner la réservation courante
            assignments.computeIfAbsent(chosen, k -> new ArrayList<>()).add(current);
            // Persister l'assignation individuelle en base
            Assignation a = new Assignation();
            a.setVehicule(chosen);
            a.setReservation(current);
            a.setDateDepart(current.calculHeureDeDepart());
            a.setDateRetour(current.calculHeureRetour());
            a.save();
            remaining.put(chosen, remaining.get(chosen) - needed);
            unassigned.remove(0);

            // Remplir autant que possible ce véhicule avec les plus grandes réservations restantes
            boolean assignedMore = true;
            while (assignedMore) {
                assignedMore = false;
                int remCap = remaining.get(chosen);
                if (remCap <= 0) break;

                // trouver la plus grande réservation restante qui rentre dans remCap
                for (int i = 0; i < unassigned.size(); i++) {
                    Reservation cand = unassigned.get(i);
                    int n = cand.getNbPassager() != null ? cand.getNbPassager() : 0;
                    if (n > 0 && n <= remCap) {
                        assignments.get(chosen).add(cand);
                        remaining.put(chosen, remCap - n);
                        // Persister l'assignation pour cette réservation
                        Assignation a2 = new Assignation();
                        a2.setVehicule(chosen);
                        a2.setReservation(cand);
                        a2.setDateDepart(cand.calculHeureDeDepart());
                        a2.setDateRetour(cand.calculHeureRetour());
                        a2.save();
                        unassigned.remove(i);
                        assignedMore = true;
                        break; // recommencer pour chercher la prochaine plus grande qui rentre
                    }
                }
            }
        }

        return assignments;
    }

    /**
     * Retourne les véhicules disponibles pendant l'intervalle [start, end).
     * Un véhicule est considéré disponible si aucune assignation n'existe
     * qui chevauche l'intervalle. (Chevauchement = NOT(date_retour <= start OR date_depart >= end))
     */
    public List<Vehicule> findAvailableVehiculesBetween(Timestamp start, Timestamp end) throws SQLException {
        List<Vehicule> available = new ArrayList<>();
        String sql = """
            SELECT v.*
            FROM vehicule v
            WHERE v.id NOT IN (
                SELECT id_vehicule FROM assignation
                WHERE NOT (date_depart >= ? OR date_retour <= ? )
            )
            ORDER BY v.nb_place DESC
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // IMPORTANT: 1 = start, 2 = end (sinon la logique de chevauchement est fausse)
            stmt.setTimestamp(1, start);
            stmt.setTimestamp(2, end);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Vehicule v = new Vehicule(
                        rs.getLong("id"),
                        rs.getString("reference"),
                        rs.getInt("nb_place"),
                        TypeCarburant.valueOf(rs.getString("type_carburant"))
                    );
                    available.add(v);
                }
            }
        }

        return available;
    }

    /**
     * Accès aux heures de départ calculées lors du dernier appel à `assignByIntervals`.
     * Valeur = heure de départ de l'intervalle auquel le véhicule a été affecté.
     */
    public Map<Vehicule, Timestamp> getLastDepartTimes() {
        return lastDepartTimes;
    }

    /**
     * Heure de départ d'un intervalle = la date_heure_arrivee la plus tardive
     * parmi les réservations assignées dans cet intervalle.
     */
    public Timestamp getDepartTimeFromAssignedReservations(List<Reservation> reservations) {
        if (reservations == null || reservations.isEmpty()) return null;
        Timestamp max = null;
        for (Reservation r : reservations) {
            if (r == null) continue;
            Timestamp t = r.getDateHeureArrivee();
            if (t == null) continue;
            if (max == null || t.after(max)) max = t;
        }
        return max;
    }

    /**
     * Assignation par intervalles temporels.
     * - Prend la liste complète des réservations (déjà triée ou non)
     * - Tant qu'il reste des réservations non traitées :
     *   * prendre la première (plus ancienne) comme début d'intervalle
     *   * construire [start, start + taMinutes]
     *   * rassembler toutes les réservations ayant date_heure_arrivee <= end
     *   * récupérer les véhicules disponibles sur l'intervalle
     *   * trier réservations desc par passagers et véhicules desc par capacité
     *   * assigner greedy en cherchant la plus petite capacité restante qui contient la resa
     *   * en cas d'égalité de capacité, utiliser `chooseByTrajetsThenCarburant`
     *   * persister chaque assignation dans la table `assignation`
     *  */
    public Map<Vehicule, List<Reservation>> assignByIntervals(List<Reservation> allReservations, int taMinutes) throws SQLException {
        Map<Vehicule, List<Reservation>> result = new HashMap<>();
        if (allReservations == null || allReservations.isEmpty()) return result;

        // reset du dernier calcul
        lastDepartTimes.clear();

        if (taMinutes <= 0) taMinutes = 30;

        // garantir l'ordre asc par date d'arrivée (utile pour découper correctement les intervalles)
        allReservations.sort((a, b) -> {
            Timestamp ta = a != null ? a.getDateHeureArrivee() : null;
            Timestamp tb = b != null ? b.getDateHeureArrivee() : null;
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });

        // s'appuyer sur la liste fournie (supposée triée asc par date d'arrivée)
        List<Reservation> unassigned = new ArrayList<>(allReservations);
        // Une réservation non assignée dans un intervalle ne redevient éligible
        // qu'à partir de la fin de cet intervalle.
        Map<Integer, Timestamp> nextEligibleByReservation = new HashMap<>();

        int maxVehicleCapacity = 0;
        for (Vehicule v : getAllVehicules()) {
            if (v != null && v.getNbPlace() > maxVehicleCapacity) {
                maxVehicleCapacity = v.getNbPlace();
            }
        }

        while (!unassigned.isEmpty()) {
            Timestamp start = findNextIntervalStart(unassigned, nextEligibleByReservation);
            if (start == null) break;
            Timestamp end = new Timestamp(start.getTime() + taMinutes * 60L * 1000L);

            List<Reservation> batch = collectBatch(unassigned, end, nextEligibleByReservation);
            if (batch.isEmpty()) continue;

            List<Vehicule> vehicles = findAvailableVehiculesBetween(start, end);
            if (vehicles.isEmpty()) {
                // Aucun véhicule disponible: on reporte tout le batch au prochain intervalle.
                for (Reservation r : batch) {
                    if (r == null || r.getIdReservation() == null) continue;
                    nextEligibleByReservation.put(r.getIdReservation(), end);
                }
                continue;
            }

            batch = sortReservationsByPassengersDesc(batch);
            vehicles = sortVehiclesByCapacityDesc(vehicles);
            Map<Vehicule, Integer> remaining = initRemaining(vehicles);

            // Pour appliquer une heure de départ commune à tout l'intervalle
            // ON NE PREND EN COMPTE QUE LES RÉSERVATIONS EFFECTIVEMENT ASSIGNÉES
            Set<Vehicule> vehiclesUsedInInterval = new HashSet<>();
            List<AssignPair> assignedPairs = new ArrayList<>();

            for (int i = 0; i < batch.size(); i++) {
                Reservation r = batch.get(i);
                int need = r.getNbPassager() != null ? r.getNbPassager() : 0;
                if (need <= 0) continue;

                Vehicule chosen = chooseVehicleForReservation(vehicles, remaining, need);
                
                if (chosen != null) {
                    // ASSIGNATION NORMALE (le véhicule a assez de place)
                    result.computeIfAbsent(chosen, k -> new ArrayList<>()).add(r);
                    remaining.put(chosen, remaining.getOrDefault(chosen, 0) - need);

                    vehiclesUsedInInterval.add(chosen);
                    assignedPairs.add(new AssignPair(chosen, r));
                } else {
                   
                    // Si aucun véhicule ne peut prendre tout le monde, on cherche celui qui a le plus de places restantes
                    Vehicule maxCapVehicle = null;
                    int maxCap = 0;
                    for (Vehicule v : vehicles) {
                        int remCap = remaining.getOrDefault(v, 0);
                        if (remCap > maxCap) {
                            maxCap = remCap;
                            maxCapVehicle = v;
                        }
                    }

                    if (maxCapVehicle != null && maxCap > 0) {
                        // Création de 2 sous-réservations
                        Reservation partToAssign = cloneReservation(r, maxCap);
                        Reservation partRemaining = cloneReservation(r, need - maxCap);

                        // On assigne la partie qui encaisse la capacité maximale du véhicule
                        result.computeIfAbsent(maxCapVehicle, k -> new ArrayList<>()).add(partToAssign);
                        remaining.put(maxCapVehicle, 0); // Le véhicule est plein
                        vehiclesUsedInInterval.add(maxCapVehicle);
                        assignedPairs.add(new AssignPair(maxCapVehicle, partToAssign));

                        // On réinsère le RESTE des passagers dans la liste à traiter juste après
                        batch.add(i + 1, partRemaining);

                        // Important: on "fake" l'assignation de la réservation d'origine 
                        // pour qu'elle soit considérée comme traitée et ne soit pas reportée indéfiniment.
                        assignedPairs.add(new AssignPair(null, r));
                    }
                }
            }

            // Calculer l'heure de départ de l'intervalle uniquement à partir des réservations assignées
            Timestamp intervalDepart = null;
            if (!assignedPairs.isEmpty()) {
                List<Reservation> assignedRes = new ArrayList<>();
                for (AssignPair p : assignedPairs) assignedRes.add(p.reservation);
                intervalDepart = getDepartTimeFromAssignedReservations(assignedRes);
            }

            // appliquer la même heure de départ à tous les véhicules affectés dans l'intervalle
            if (intervalDepart != null && !vehiclesUsedInInterval.isEmpty()) {
                for (Vehicule v : vehiclesUsedInInterval) {
                    // si le véhicule est utilisé plusieurs fois dans la journée, on garde son 1er départ
                    Timestamp existing = lastDepartTimes.get(v);
                    if (existing == null || intervalDepart.before(existing)) {
                        lastDepartTimes.put(v, intervalDepart);
                    }
                }

                // Regrouper les réservations par véhicule pour calculer l'heure de retour commune
                Map<Vehicule, List<Reservation>> vehicleReservations = new HashMap<>();
                for (AssignPair pair : assignedPairs) {
                    vehicleReservations.computeIfAbsent(pair.vehicule, k -> new ArrayList<>()).add(pair.reservation);
                }

                // Persister les assignations avec l'heure de retour calculée à partir du départ commun
                for (Map.Entry<Vehicule, List<Reservation>> entry : vehicleReservations.entrySet()) {
                    Vehicule v = entry.getKey();
                    List<Reservation> vReservations = entry.getValue();

                    // Calculer l'heure de retour commune pour ce véhicule
                    Timestamp vehicleRetour = null;
                    try {
                        vehicleRetour = Reservation.calculHeureRetourFromDepart(intervalDepart, vReservations);
                    } catch (SQLException ignore) {
                    }

                    // Persister chaque assignation avec la même heure de retour
                    for (Reservation r : vReservations) {
                        persistAssignationWithRetour(v, r, intervalDepart, vehicleRetour);
                    }
                }
            }

            // Retirer uniquement les réservations qui ont été assignées.
            // Les réservations non-assignées sont renvoyées en fin de liste
            // pour être reconsidérées au prochain intervalle (cycle continu).
            Set<Integer> assignedIds = new HashSet<>();
            for (AssignPair p : assignedPairs) {
                if (p.reservation != null && p.reservation.getIdReservation() != null) {
                    assignedIds.add(p.reservation.getIdReservation());
                }
            }

            for (Reservation r : batch) {
                if (r == null) continue;
                Integer id = r.getIdReservation();
                if (id != null && assignedIds.contains(id)) {
                    unassigned.remove(r);
                } else {
                    // Réservation non assignée : reporter au prochain intervalle
                    // Elle sera reconsidérée avec les nouvelles réservations de cet intervalle
                    if (id != null) {
                        nextEligibleByReservation.put(id, end);
                    }
                }
            }

            // Vérifier s'il reste des intervalles avec des réservations "naturelles"
            // Une réservation "naturelle" est une réservation dont l'heure d'arrivée réelle
            // est après l'intervalle courant (donc pas encore traitée)
            boolean hasNaturalReservationsLeft = false;
            for (Reservation r : unassigned) {
                if (r == null) continue;
                Integer id = r.getIdReservation();
                Timestamp arrival = r.getDateHeureArrivee();
                if (arrival != null && arrival.getTime() >= end.getTime()) {
                    // Cette réservation n'a pas encore été traitée naturellement
                    Timestamp eligible = nextEligibleByReservation.get(id);
                    if (eligible == null) {
                        // C'est une réservation naturelle (pas reportée)
                        hasNaturalReservationsLeft = true;
                        break;
                    }
                }
            }

            // Si plus aucune réservation naturelle, retirer les reportées définitivement
            if (!hasNaturalReservationsLeft) {
                List<Reservation> toRemove = new ArrayList<>();
                for (Reservation r : unassigned) {
                    if (r == null) continue;
                    Integer id = r.getIdReservation();
                    if (id != null && nextEligibleByReservation.containsKey(id)) {
                        toRemove.add(r);
                    }
                }
                unassigned.removeAll(toRemove);
            }
        }

        return result;
    }
       private static class AssignPair {
        private final Vehicule vehicule;
        private final Reservation reservation;

        private AssignPair(Vehicule vehicule, Reservation reservation) {
            this.vehicule = vehicule;
            this.reservation = reservation;
        }
    }

    private Timestamp getEffectiveArrival(Reservation r, Map<Integer, Timestamp> nextEligibleByReservation) {
        if (r == null) return null;
        Timestamp arrival = r.getDateHeureArrivee();
        Integer id = r.getIdReservation();
        if (arrival == null || id == null) return arrival;

        Timestamp eligible = nextEligibleByReservation.get(id);
        if (eligible != null && eligible.after(arrival)) {
            return eligible;
        }
        return arrival;
    }

    private Timestamp findNextIntervalStart(List<Reservation> unassigned, Map<Integer, Timestamp> nextEligibleByReservation) {
        Timestamp min = null;
        List<Reservation> invalid = new ArrayList<>();

        // Ne prendre que les réservations NATURELLES (non reportées) pour déterminer le début de l'intervalle
        for (Reservation r : unassigned) {
            if (r == null) {
                invalid.add(r);
                continue;
            }
            Integer id = r.getIdReservation();
            // Ignorer les réservations reportées pour le calcul du start
            if (id != null && nextEligibleByReservation.containsKey(id)) {
                continue;
            }
            Timestamp arrival = r.getDateHeureArrivee();
            if (arrival == null) {
                invalid.add(r);
                continue;
            }
            if (min == null || arrival.before(min)) {
                min = arrival;
            }
        }

        if (!invalid.isEmpty()) {
            unassigned.removeAll(invalid);
        }

        return min;
    }

    // Helper: collecte les réservations éligibles dont la date d'arrivée effective <= end
    private List<Reservation> collectBatch(List<Reservation> unassigned, Timestamp end, Map<Integer, Timestamp> nextEligibleByReservation) {
        List<Reservation> batch = new ArrayList<>();
        for (Reservation r : unassigned) {
            Timestamp t = getEffectiveArrival(r, nextEligibleByReservation);
            if (t != null && t.getTime() <= end.getTime()) batch.add(r);
        }
        return batch;
    }

    private List<Reservation> sortReservationsByPassengersDesc(List<Reservation> batch) {
        batch.sort((a, b) -> {
            int na = a.getNbPassager() != null ? a.getNbPassager() : 0;
            int nb = b.getNbPassager() != null ? b.getNbPassager() : 0;
            return Integer.compare(nb, na);
        });
        return batch;
    }

    private List<Vehicule> sortVehiclesByCapacityDesc(List<Vehicule> vehicles) {
        vehicles.sort((v1, v2) -> Integer.compare(v2.getNbPlace(), v1.getNbPlace()));
        return vehicles;
    }

    private Map<Vehicule, Integer> initRemaining(List<Vehicule> vehicles) {
        Map<Vehicule, Integer> remaining = new HashMap<>();
        for (Vehicule v : vehicles) remaining.put(v, v.getNbPlace());
        return remaining;
    }

    private Vehicule chooseVehicleForReservation(List<Vehicule> vehicles, Map<Vehicule, Integer> remaining, int need) {
        if (vehicles == null || vehicles.isEmpty()) return null;
        int bestCap = Integer.MAX_VALUE;
        List<Vehicule> ties = new ArrayList<>();
        for (Vehicule v : vehicles) {
            int rem = remaining.getOrDefault(v, 0);
            if (rem >= need) {
                if (rem < bestCap) {
                    bestCap = rem;
                    ties.clear();
                    ties.add(v);
                } else if (rem == bestCap) {
                    ties.add(v);
                }
            }
        }
        if (ties.isEmpty()) return null;
        if (ties.size() == 1) return ties.get(0);
        Vehicule chosen = chooseByTrajetsThenCarburant(ties);
        if (chosen != null) return chosen;
        return ties.get(new Random().nextInt(ties.size()));
    }

    private void persistAssignation(Vehicule chosen, Reservation r, Timestamp intervalDepart) {
        Assignation a = new Assignation();
        a.setVehicule(chosen);
        a.setReservation(r);
        // Départ commun à l'intervalle
        a.setDateDepart(intervalDepart);

        try {
            a.setDateRetour(r.calculHeureRetour());
        } catch (Exception ignore) {
            // laisser null si non calculable
        }

        // Eviter de créer des assignations avec dates NULL (ça fausse la disponibilité SQL)
        try {
            if (a.getDateDepart() != null && a.getDateRetour() != null) {
                a.save();
            }
        } catch (SQLException ignore) {
        }
    }

    /**
     * Persiste une assignation avec une heure de retour pré-calculée
     * (basée sur le départ commun de l'intervalle et la distance totale du trajet).
     */
    private void persistAssignationWithRetour(Vehicule chosen, Reservation r, Timestamp intervalDepart, Timestamp vehicleRetour) {
        Assignation a = new Assignation();
        a.setVehicule(chosen);
        a.setReservation(r);
        a.setDateDepart(intervalDepart);
        a.setDateRetour(vehicleRetour);

        // Eviter de créer des assignations avec dates NULL (ça fausse la disponibilité SQL)
        try {
            if (a.getDateDepart() != null && a.getDateRetour() != null) {
                a.save();
            }
        } catch (SQLException ignore) {
        }
    }

// ...existing code...
    /**
     * Planification principale pour une date :
     * - récupère les réservations du jour
     * - délègue l'assignation aux véhicules à `assignVehiculeToReservation`
     */
    public Map<Vehicule, List<Reservation>> planifyByDate(Timestamp date) throws SQLException {
        if (date == null) return new HashMap<>();
        List<Reservation> reservations = Reservation.findReservationsByDateASC(date);
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("Aucune réservation trouvée pour la date: " + date);
        }

        ParametreRepository repo = new ParametreRepository();
        Parametre p = repo.findLatest();
        if (p == null || p.getTempsAttenteMin() == null || p.getTempsAttenteMin() <= 0) {
            throw new IllegalArgumentException("Paramètre tempsAttenteMin invalide ou manquant. Veuillez vérifier la table parametre.");
        }

        return assignByIntervals(reservations, p.getTempsAttenteMin());
    }

    /**
     * Retourne la liste des réservations qui ne figurent pas dans la map d'assignations.
     * La comparaison se fait par `idReservation`.
     */
    public List<Reservation> findUnassignedReservations(List<Reservation> reservations, Map<Vehicule, List<Reservation>> assignments) {
        List<Reservation> result = new ArrayList<>();
        if (reservations == null || reservations.isEmpty()) return result;

        Set<Integer> assignedIds = new HashSet<>();
        if (assignments != null && !assignments.isEmpty()) {
            for (List<Reservation> assignes : assignments.values()) {
                if (assignes == null) continue;
                for (Reservation resa : assignes) {
                    if (resa != null && resa.getIdReservation() != null) assignedIds.add(resa.getIdReservation());
                }
            }
        }

        for (Reservation resa : reservations) {
            Integer id = resa != null ? resa.getIdReservation() : null;
            if (id == null || !assignedIds.contains(id)) {
                result.add(resa);
            }
        }

        return result;
    }

    /**
    
     * 
     * Les hôtels sont triés par distance depuis l'aéroport (plus proche d'abord).
     * La distance est cherchée dans les 2 sens (from→to et to→from).
     * 
     * @param reservations Liste des réservations assignées à un véhicule
     * @return La distance totale en km (BigDecimal)
     * @throws SQLException En cas d'erreur de base de données
     */
    public BigDecimal calculTotalDistance(List<Reservation> reservations) throws SQLException {
        if (reservations == null || reservations.isEmpty()) {
            return BigDecimal.ZERO;
        }

        DistanceRepository distanceRepo = new DistanceRepository();

        // 1. Trouver l'aéroport (hotel avec aeroport = true)
        Integer aeroportId = null;
        for (Hotel h : Hotel.findAll()) {
            if (Boolean.TRUE.equals(h.getAeroport())) {
                aeroportId = h.getIdHotel();
                break;
            }
        }
        if (aeroportId == null) {
            throw new SQLException("Aucun aéroport trouvé dans la table hotel");
        }

        // 2. Récupérer les hôtels uniques des réservations
        List<Integer> hotelIds = new ArrayList<>();
        Set<Integer> hotelIdSet = new HashSet<>();
        for (Reservation resa : reservations) {
            if (resa.getHotel() != null) {
                int hotelId = resa.getHotel().getIdHotel();
                if (!hotelIdSet.contains(hotelId)) {
                    hotelIdSet.add(hotelId);
                    hotelIds.add(hotelId);
                }
            }
        }

        if (hotelIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 3. Construire l'ordre optimal des hôtels : commencer à l'aéroport, aller au plus proche, puis du courant au plus proche restant, etc.
        List<Integer> orderedHotels = new ArrayList<>();
        Set<Integer> remaining = new HashSet<>(hotelIds);
        int current = aeroportId;
        while (!remaining.isEmpty()) {
            Integer next = null;
            BigDecimal minDist = null;
            String nextName = null;
            for (Integer h : remaining) {
                BigDecimal d = getDistanceKm(distanceRepo, current, h);
                if (d != null) {
                    boolean isBetter = false;
                    if (minDist == null || d.compareTo(minDist) < 0) {
                        isBetter = true;
                    } else if (d.compareTo(minDist) == 0) {
                        // même distance, comparer noms alphabétiquement
                        try {
                            Hotel hObj = Hotel.findById(h);
                            String hName = hObj != null ? hObj.getLibelle() : "";
                            if (nextName == null || hName.compareTo(nextName) < 0) {
                                isBetter = true;
                            }
                        } catch (SQLException e) {
                        }
                    }
                    if (isBetter) {
                        minDist = d;
                        next = h;
                        try {
                            Hotel hObj = Hotel.findById(h);
                            nextName = hObj != null ? hObj.getLibelle() : "";
                        } catch (SQLException e) {
                            nextName = "";
                        }
                    }
                }
            }
            if (next != null) {
                orderedHotels.add(next);
                remaining.remove(next);
                current = next;
            } else {
                // si pas trouvé, ajouter le premier restant
                if (!remaining.isEmpty()) {
                    next = remaining.iterator().next();
                    orderedHotels.add(next);
                    remaining.remove(next);
                    current = next;
                }
            }
        }

        // 4. Calculer la distance totale : Aéroport → Hôtel1 → Hôtel2 → ... → Aéroport
        BigDecimal totalDistance = BigDecimal.ZERO;

        // Aéroport → Premier hôtel
        if (!orderedHotels.isEmpty()) {
            totalDistance = totalDistance.add(getDistanceKm(distanceRepo, aeroportId, orderedHotels.get(0)));
        }

        // Hôtel1 → Hôtel2 → ... → Dernier hôtel
        for (int i = 0; i < orderedHotels.size() - 1; i++) {
            totalDistance = totalDistance.add(getDistanceKm(distanceRepo, orderedHotels.get(i), orderedHotels.get(i + 1)));
        }

        // Dernier hôtel → Aéroport
        if (!orderedHotels.isEmpty()) {
            totalDistance = totalDistance.add(getDistanceKm(distanceRepo, orderedHotels.get(orderedHotels.size() - 1), aeroportId));
        }

        return totalDistance;
    }

    /**
     * Récupère la distance en km entre 2 hôtels.
     * Cherche dans les 2 sens : from→to puis to→from.
     * 
     * @param distanceRepo Le repository Distance
     * @param fromHotelId Hôtel de départ
     * @param toHotelId Hôtel d'arrivée
     * @return Distance en km (BigDecimal)
     * @throws SQLException Si distance non trouvée
     */
    private BigDecimal getDistanceKm(DistanceRepository distanceRepo, int fromHotelId, int toHotelId) throws SQLException {
        if (fromHotelId == toHotelId) {
            return BigDecimal.ZERO;
        }

        // Chercher dans le sens from → to
        Distance d = distanceRepo.findDistance(fromHotelId, toHotelId);
        if (d != null && d.getKilometre() != null) {
            return d.getKilometre();
        }

        // Chercher dans le sens inverse to → from
        d = distanceRepo.findDistance(toHotelId, fromHotelId);
        if (d != null && d.getKilometre() != null) {
            return d.getKilometre();
        }

        throw new SQLException("Distance non trouvée entre hotel " + fromHotelId + " et hotel " + toHotelId);
    }

    /**
     * Génère la description du trajet pour une liste de réservations.
     * Utilise l'algorithme du plus proche voisin : départ de l'aéroport,
     * puis vers l'hôtel le plus proche, et ainsi de suite.
     * En cas d'égalité de distance, ordre alphabétique des noms d'hôtels.
     * 
     * @param reservations Liste des réservations
     * @return Description du trajet (ex: "Aéroport -> Hôtel A -> Hôtel B -> Aéroport")
     * @throws SQLException En cas d'erreur de base de données
     */
    public String getRouteDescription(List<Reservation> reservations) throws SQLException {
        if (reservations == null || reservations.isEmpty()) {
            return "Aéroport -> Aéroport";
        }

        // Trouver l'aéroport
        Integer aeroportId = null;
        for (Hotel h : Hotel.findAll()) {
            if (Boolean.TRUE.equals(h.getAeroport())) {
                aeroportId = h.getIdHotel();
                break;
            }
        }
        if (aeroportId == null) {
            return "Aéroport -> Aéroport";
        }

        // Récupérer les hôtels uniques
        Set<Integer> hotelIdSet = new HashSet<>();
        for (Reservation r : reservations) {
            if (r.getHotel() != null) {
                hotelIdSet.add(r.getHotel().getIdHotel());
            }
        }
        List<Integer> hotelIds = new ArrayList<>(hotelIdSet);
        if (hotelIds.isEmpty()) {
            return "Aéroport -> Aéroport";
        }

        // Construire l'ordre optimal avec l'algorithme du plus proche voisin
        DistanceRepository dr = new DistanceRepository();
        List<Integer> orderedHotels = new ArrayList<>();
        Set<Integer> remaining = new HashSet<>(hotelIds);
        int current = aeroportId;
        while (!remaining.isEmpty()) {
            Integer next = null;
            BigDecimal minDist = null;
            String nextName = null;
            for (Integer h : remaining) {
                BigDecimal d = getDistanceKm(dr, current, h);
                if (d != null) {
                    boolean isBetter = false;
                    if (minDist == null || d.compareTo(minDist) < 0) {
                        isBetter = true;
                    } else if (d.compareTo(minDist) == 0) {
                        // Même distance, comparer noms alphabétiquement
                        try {
                            Hotel hObj = Hotel.findById(h);
                            String hName = hObj != null ? hObj.getLibelle() : "";
                            if (nextName == null || hName.compareTo(nextName) < 0) {
                                isBetter = true;
                            }
                        } catch (Exception e) {}
                    }
                    if (isBetter) {
                        minDist = d;
                        next = h;
                        try {
                            Hotel hObj = Hotel.findById(h);
                            nextName = hObj != null ? hObj.getLibelle() : "";
                        } catch (Exception e) {
                            nextName = "";
                        }
                    }
                }
            }
            if (next != null) {
                orderedHotels.add(next);
                remaining.remove(next);
                current = next;
            } else {
                // Si pas trouvé, ajouter le premier restant
                if (!remaining.isEmpty()) {
                    next = remaining.iterator().next();
                    orderedHotels.add(next);
                    remaining.remove(next);
                    current = next;
                }
            }
        }

        // Construire la chaîne
        StringBuilder sb = new StringBuilder("Aéroport");
        for (Integer hid : orderedHotels) {
            try {
                Hotel h = Hotel.findById(hid);
                if (h != null) {
                    sb.append(" -> ").append(h.getLibelle());
                }
            } catch (Exception e) {}
        }
        sb.append(" -> Aéroport");
        return sb.toString();
    }

    /**
     * Choisit un véhicule parmi les candidats.
     * Si tous les candidats ont le même type de carburant, choix aléatoire.
     * Sinon, privilégie le diesel.
     */
    private Vehicule chooseFromCandidates(List<Vehicule> candidates) {
        if (candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        // Vérifier si tous ont le même type de carburant
        TypeCarburant first = candidates.get(0).getTypeCarburant();
        boolean allSame = true;
        for (Vehicule v : candidates) {
            if (v.getTypeCarburant() != first) {
                allSame = false;
                break;
            }
        }

        if (allSame) {
            // Choix aléatoire
            Random rand = new Random();
            return candidates.get(rand.nextInt(candidates.size()));
        } else {
            // Privilégier le diesel
            for (Vehicule v : candidates) {
                if (v.getTypeCarburant() == TypeCarburant.DIESEL) {
                    return v;
                }
            }
            // Si pas de diesel, prendre le premier
            return candidates.get(0);
        }
    }

    /**
     * Choisit parmi des candidats en priorisant : moins de trajets effectués,
     * puis priorité carburant (diesel), puis aléatoire.
     */
    private Vehicule chooseByTrajetsThenCarburant(List<Vehicule> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (candidates.size() == 1) return candidates.get(0);

        try {
            // calculer le nombre de trajets pour chaque véhicule
            int best = Integer.MAX_VALUE;
            List<Vehicule> bestCandidates = new ArrayList<>();
            for (Vehicule v : candidates) {
                Integer cnt = 0;
                try {
                    cnt = vehiculeRepository.countTrajets(v.getId());
                } catch (SQLException e) {
                    cnt = Integer.MAX_VALUE; // en cas d'erreur, ne pas privilégier
                }
                if (cnt < best) {
                    best = cnt;
                    bestCandidates.clear();
                    bestCandidates.add(v);
                } else if (cnt == best) {
                    bestCandidates.add(v);
                }
            }

            if (bestCandidates.size() == 1) return bestCandidates.get(0);
            // si plusieurs, déléguer à la logique carburant + aléatoire
            return chooseFromCandidates(bestCandidates);
        } catch (Exception e) {
            // En cas de problème, fallback
            return chooseFromCandidates(candidates);
        }
    }

    public Timestamp getHeureRetour(Long vehiculeId) throws SQLException {
        return vehiculeRepository.getLastReturnDate(vehiculeId);
    }

    public Integer countTrajets(Long vehiculeId) throws SQLException {
        return vehiculeRepository.countTrajets(vehiculeId);
    }
    private Reservation cloneReservation(Reservation r, int newNbPassagers) {
        if (r == null) return null;
        
        // On utilise le constructeur complet de votre entité Reservation:
        // public Reservation(Integer idReservation, Integer nbPassager, Timestamp dateHeureArrivee, Hotel hotel, String idClient)
        return new Reservation(
            r.getIdReservation(),
            newNbPassagers, 
            r.getDateHeureArrivee(),
            r.getHotel(),
            r.getIdClient()
        );
    }
}