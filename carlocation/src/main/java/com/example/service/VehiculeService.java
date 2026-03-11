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
import java.util.Comparator;
import java.util.Random;

import com.example.entity.Distance;
import com.example.entity.Hotel;
import com.example.repository.DistanceRepository;

public class VehiculeService {
    
    private final VehiculeRepository vehiculeRepository;

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
                        // si même capacité, utiliser la priorisation carburant existante
                        if (chosen != null) {
                            List<Vehicule> tie = new ArrayList<>();
                            tie.add(chosen);
                            tie.add(v);
                            chosen = chooseFromCandidates(tie);
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
                        unassigned.remove(i);
                        assignedMore = true;
                        break; // recommencer pour chercher la prochaine plus grande qui rentre
                    }
                }
            }
        }

        return assignments;
    }

// ...existing code...
    /**
     * Planification principale pour une date :
     * - récupère les réservations du jour
     * - délègue l'assignation aux véhicules à `assignVehiculeToReservation`
     */
    public Map<Vehicule, List<Reservation>> planifyByDate(Timestamp date) throws SQLException {
        if (date == null) return new HashMap<>();
        List<Reservation> reservations = Reservation.findReservationsByDate(date);
        if (reservations.isEmpty()) {
            throw new IllegalArgumentException("Aucune réservation trouvée pour la date: " + date);
        }
        
        return assignVehiculeToReservation(reservations);
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

}