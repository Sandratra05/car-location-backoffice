package com.example.entity;

import com.example.config.DbConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Reservation {

    private Integer idReservation;
    private Integer nbPassager;
    private LocalDateTime dateHeureArrivee;
    private Integer idHotel;
    private String idClient;

    public Reservation() {
    }

    public Reservation(Integer idReservation, Integer nbPassager, LocalDateTime dateHeureArrivee,
                       Integer idHotel, String idClient) {
        this.idReservation = idReservation;
        this.nbPassager = nbPassager;
        this.dateHeureArrivee = dateHeureArrivee;
        this.idHotel = idHotel;
        this.idClient = idClient;
    }

    public Integer getIdReservation() {
        return idReservation;
    }

    public void setIdReservation(Integer idReservation) {
        this.idReservation = idReservation;
    }

    public Integer getNbPassager() {
        return nbPassager;
    }

    public void setNbPassager(Integer nbPassager) {
        this.nbPassager = nbPassager;
    }

    public LocalDateTime getDateHeureArrivee() {
        return dateHeureArrivee;
    }

    public void setDateHeureArrivee(LocalDateTime dateHeureArrivee) {
        this.dateHeureArrivee = dateHeureArrivee;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public void save() throws SQLException {
        String sql = "INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, this.nbPassager);
            stmt.setTimestamp(2, Timestamp.valueOf(this.dateHeureArrivee));
            stmt.setInt(3, this.idHotel);
            stmt.setString(4, this.idClient);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                this.idReservation = rs.getInt(1);
            }
        }
    }

    public static List<Reservation> findAll() throws SQLException {
        List<Reservation> reservations = new ArrayList<>();
        String sql = "SELECT * FROM reservation";

        try (Connection conn = DbConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Reservation r = new Reservation();
                r.setIdReservation(rs.getInt("id_reservation"));
                r.setNbPassager(rs.getInt("nb_passager"));
                r.setDateHeureArrivee(rs.getTimestamp("date_heure_arrivee").toLocalDateTime());
                r.setIdHotel(rs.getInt("id_hotel"));
                r.setIdClient(rs.getString("id_client"));
                reservations.add(r);
            }
        }
        return reservations;
    }

}
