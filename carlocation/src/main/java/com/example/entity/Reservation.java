package com.example.entity;

import com.example.config.DbConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Reservation {

    private Integer idReservation;
    private Integer nbPassager;
    private Timestamp dateHeureArrivee;
    private Hotel hotel;
    private String idClient;

    public Reservation() {}

    public Reservation(Integer idReservation, Integer nbPassager,
                       Timestamp dateHeureArrivee, Hotel hotel, String idClient) {
        this.idReservation = idReservation;
        this.nbPassager = nbPassager;
        this.dateHeureArrivee = dateHeureArrivee;
        this.hotel = hotel;
        this.idClient = idClient;
    }

    public Integer getIdReservation() {
        return idReservation;
    }

    public Integer getNbPassager() {
        return nbPassager;
    }

    public void setNbPassager(Integer nbPassager) {
        this.nbPassager = nbPassager;
    }

    public Timestamp getDateHeureArrivee() {
        return dateHeureArrivee;
    }

    public void setDateHeureArrivee(Timestamp dateHeureArrivee) {
        this.dateHeureArrivee = dateHeureArrivee;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getIdClient() {
        return idClient;
    }

    public void setIdClient(String idClient) {
        this.idClient = idClient;
    }

    public void save() throws SQLException {
        String sql = """
            INSERT INTO reservation (nb_passager, date_heure_arrivee, id_hotel, id_client)
            VALUES (?, ?, ?, ?)
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, nbPassager);
            stmt.setTimestamp(2, dateHeureArrivee);
            stmt.setInt(3, hotel.getIdHotel());
            stmt.setString(4, idClient);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                idReservation = rs.getInt(1);
            }
        }
    }

    public static List<Reservation> findAll() throws SQLException {
        List<Reservation> list = new ArrayList<>();

        String sql = """
            SELECT r.*, h.libelle, h.distance
            FROM reservation r
            JOIN hotel h ON r.id_hotel = h.id_hotel
        """;

        try (Connection conn = DbConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Hotel hotel = new Hotel(
                        rs.getInt("id_hotel"),
                        rs.getString("libelle"),
                        rs.getDouble("distance")
                );

                Reservation r = new Reservation();
                r.idReservation = rs.getInt("id_reservation");
                r.nbPassager = rs.getInt("nb_passager");
                r.dateHeureArrivee = rs.getTimestamp("date_heure_arrivee");
                r.hotel = hotel;
                r.idClient = rs.getString("id_client");

                list.add(r);
            }
        }
        return list;
    }
}
