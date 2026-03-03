package com.example.service;

import com.example.config.DbConnection;
import com.example.entity.Reservation;
import com.example.entity.Vehicule;
import com.example.enums.TypeCarburant;
import com.example.repository.VehiculeRepository;

import java.util.Map;
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

        // Assume all reservations are for the same date
        List<Vehicule> allVehicules = getAllVehicules();

        Map<Vehicule, List<Reservation>> assignments = new HashMap<>();
        // remaining capacity per vehicle (initially full capacity)
        Map<Vehicule, Integer> remaining = new HashMap<>();
        for (Vehicule v : allVehicules) {
            remaining.put(v, v.getNbPlace());
        }

        // Sort reservations by arrival time ascending: first incoming passengers are boarded first
        reservations.sort((a, b) -> {
            Timestamp ta = a.getDateHeureArrivee();
            Timestamp tb = b.getDateHeureArrivee();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return ta.compareTo(tb);
        });

        for (Reservation resa : reservations) {
            int needed = resa.getNbPassager() != null ? resa.getNbPassager() : 0;
            if (needed <= 0) continue;

            Vehicule chosen = null;

            // 1) Try to fit into already assigned vehicles (fill current vehicles first)
            List<Vehicule> assignedCandidates = new ArrayList<>();
            for (Vehicule v : assignments.keySet()) {
                Integer rem = remaining.getOrDefault(v, v.getNbPlace());
                if (rem >= needed) assignedCandidates.add(v);
            }

            if (!assignedCandidates.isEmpty()) {
                // pick the best fit among assigned vehicles: minimal remaining capacity >= needed
                int bestRem = Integer.MAX_VALUE;
                for (Vehicule v : assignedCandidates) {
                    int rem = remaining.get(v);
                    if (rem < bestRem) bestRem = rem;
                }

                // collect those with bestRem and prefer DIESEL
                Vehicule dieselCandidate = null;
                for (Vehicule v : assignedCandidates) {
                    if (remaining.get(v) == bestRem) {
                        if (v.getTypeCarburant() == TypeCarburant.DIESEL) {
                            dieselCandidate = v;
                            break;
                        }
                        if (chosen == null) chosen = v;
                    }
                }
                if (dieselCandidate != null) chosen = dieselCandidate;
                if (chosen != null) {
                    assignments.get(chosen).add(resa);
                    remaining.put(chosen, remaining.get(chosen) - needed);
                    continue;
                }
            }

            // 2) Otherwise, pick among unused or not-yet-filled vehicles
            // Find minimal vehicle capacity that can carry the reservation (consider full capacity for unassigned)
            int capacityMin = Integer.MAX_VALUE;
            for (Vehicule v : allVehicules) {
                int cap = v.getNbPlace();
                // consider current remaining (could be full for unassigned)
                int rem = remaining.getOrDefault(v, cap);
                // only consider vehicles that can carry the whole reservation in one go
                if (rem >= needed && cap < capacityMin) {
                    capacityMin = cap;
                }
            }

            if (capacityMin == Integer.MAX_VALUE) {
                // no vehicle can carry this reservation entirely
                continue;
            }

            List<Vehicule> candidates = new ArrayList<>();
            for (Vehicule v : allVehicules) {
                if (v.getNbPlace() == capacityMin && remaining.getOrDefault(v, v.getNbPlace()) >= needed) {
                    candidates.add(v);
                }
            }

            if (candidates.isEmpty()) {
                continue;
            }

            // prefer DIESEL among candidates
            Vehicule diesel = null;
            for (Vehicule c : candidates) {
                if (c.getTypeCarburant() == TypeCarburant.DIESEL) {
                    diesel = c;
                    break;
                }
            }
            if (diesel != null) chosen = diesel; else chosen = candidates.get(0);

            assignments.computeIfAbsent(chosen, k -> new ArrayList<>()).add(resa);
            remaining.put(chosen, remaining.getOrDefault(chosen, chosen.getNbPlace()) - needed);
            // in-memory only: do not persist to DB
        }

        return assignments;
    }

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

}