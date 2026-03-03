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
        List<Vehicule> availableVehicules = getAllVehicules();

        Map<Vehicule, List<Reservation>> assignments = new HashMap<>();
        // Sort reservations to assign larger groups first (helps fit big groups)
        reservations.sort((a, b) -> Integer.compare(b.getNbPassager(), a.getNbPassager()));

        for (Reservation resa : reservations) {
            // 1) find minimal capacity that can carry the reservation
            int capacityMin = Integer.MAX_VALUE;
            for (Vehicule vehicule : availableVehicules) {
                int capacaityVehicule = vehicule.getNbPlace();
                if (capacaityVehicule >= resa.getNbPassager() && capacaityVehicule < capacityMin) {
                    capacityMin = capacaityVehicule;
                }
            }

            if (capacityMin == Integer.MAX_VALUE) {
                // no vehicle can carry this reservation
                continue;
            }

            // 2) collect candidates with capacity == capacityMin
            List<Vehicule> candidates = new ArrayList<>();
            for (Vehicule vehicule : availableVehicules) {
                if (vehicule.getNbPlace() == capacityMin) {
                    candidates.add(vehicule);
                }
            }

            if (candidates.isEmpty()) {
                continue;
            }

            // 3) prefer DIESEL among candidates
            Vehicule chosen = null;
            for (Vehicule c : candidates) {
                if (c.getTypeCarburant() == TypeCarburant.DIESEL) {
                    chosen = c;
                    break;
                }
            }
            if (chosen == null) {
                chosen = candidates.get(0);
            }

            assignments.computeIfAbsent(chosen, k -> new ArrayList<>()).add(resa);
            // in-memory only: do not persist to DB
        }

        return assignments;
    }

}