package com.example.entity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.example.config.DbConnection;

public class Hotel {
    private Integer idHotel;
    private String libelle;
    private Double distance;

    public Hotel() {
    }

    public Hotel(Integer idHotel, String libelle, Double distance) {
        this.idHotel = idHotel;
        this.libelle = libelle;
        this.distance = distance;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public static List<Hotel> findAll() throws SQLException {
        List<Hotel> hotels = new ArrayList<>();
        String sql = "SELECT * FROM hotel";
        try (Connection conn = DbConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Hotel hotel = new Hotel();
                hotel.setIdHotel(rs.getInt("id_hotel"));
                hotel.setLibelle(rs.getString("libelle"));
                hotel.setDistance(rs.getDouble("distance"));
                hotels.add(hotel);
            }
        }
        return hotels;
    }

    public static Hotel findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM hotel WHERE id_hotel = ?";
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Hotel hotel = new Hotel();
                hotel.setIdHotel(rs.getInt("id_hotel"));
                hotel.setLibelle(rs.getString("libelle"));
                hotel.setDistance(rs.getDouble("distance"));
                return hotel;
            }
        }
        return null;
    }
}
