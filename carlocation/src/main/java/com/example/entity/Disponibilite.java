package com.example.entity;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.example.config.DbConnection;

/**
 * Entité représentant la disponibilité d'un véhicule pour une date donnée.
 * Permet de définir qu'un véhicule ne sera disponible qu'à partir d'une certaine heure.
 */
public class Disponibilite {
    private Integer id;
    private Integer idVehicule;
    private Date dateDisponible;
    private Time heureDisponible;

    public Disponibilite() {}

    public Disponibilite(Integer id, Integer idVehicule, Date dateDisponible, Time heureDisponible) {
        this.id = id;
        this.idVehicule = idVehicule;
        this.dateDisponible = dateDisponible;
        this.heureDisponible = heureDisponible;
    }

    // Getters et Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdVehicule() {
        return idVehicule;
    }

    public void setIdVehicule(Integer idVehicule) {
        this.idVehicule = idVehicule;
    }

    public Date getDateDisponible() {
        return dateDisponible;
    }

    public void setDateDisponible(Date dateDisponible) {
        this.dateDisponible = dateDisponible;
    }

    public Time getHeureDisponible() {
        return heureDisponible;
    }

    public void setHeureDisponible(Time heureDisponible) {
        this.heureDisponible = heureDisponible;
    }

    /**
     * Retourne le Timestamp combinant date et heure de disponibilité
     */
    public Timestamp getTimestampDisponible() {
        if (dateDisponible == null || heureDisponible == null) return null;
        return Timestamp.valueOf(dateDisponible.toLocalDate().atTime(heureDisponible.toLocalTime()));
    }

    /**
     * Trouve la disponibilité d'un véhicule pour une date donnée
     */
    public static Disponibilite findByVehiculeAndDate(Long idVehicule, Date date) throws SQLException {
        String sql = "SELECT * FROM disponibilite WHERE id_vehicule = ? AND date_disponible = ?";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, idVehicule);
            stmt.setDate(2, date);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Disponibilite(
                        rs.getInt("id"),
                        rs.getInt("id_vehicule"),
                        rs.getDate("date_disponible"),
                        rs.getTime("heure_disponible")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Trouve toutes les disponibilités pour une date donnée
     */
    public static List<Disponibilite> findByDate(Date date) throws SQLException {
        List<Disponibilite> list = new ArrayList<>();
        String sql = "SELECT * FROM disponibilite WHERE date_disponible = ?";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, date);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new Disponibilite(
                        rs.getInt("id"),
                        rs.getInt("id_vehicule"),
                        rs.getDate("date_disponible"),
                        rs.getTime("heure_disponible")
                    ));
                }
            }
        }
        return list;
    }

    /**
     * Sauvegarde ou met à jour la disponibilité
     */
    public void save() throws SQLException {
        if (id == null) {
            String sql = "INSERT INTO disponibilite (id_vehicule, date_disponible, heure_disponible) VALUES (?, ?, ?) RETURNING id";
            try (Connection conn = DbConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, idVehicule);
                stmt.setDate(2, dateDisponible);
                stmt.setTime(3, heureDisponible);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        this.id = rs.getInt("id");
                    }
                }
            }
        } else {
            String sql = "UPDATE disponibilite SET id_vehicule = ?, date_disponible = ?, heure_disponible = ? WHERE id = ?";
            try (Connection conn = DbConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, idVehicule);
                stmt.setDate(2, dateDisponible);
                stmt.setTime(3, heureDisponible);
                stmt.setInt(4, id);

                stmt.executeUpdate();
            }
        }
    }

    /**
     * Supprime la disponibilité
     */
    public void delete() throws SQLException {
        if (id == null) return;

        String sql = "DELETE FROM disponibilite WHERE id = ?";
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }
}
