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
    private String code;
    private String libelle;
    private Boolean aeroport;

    public Hotel() {
    }

    public Hotel(Integer idHotel, String code, String libelle, Boolean aeroport) {
        this.idHotel = idHotel;
        this.code = code;
        this.libelle = libelle;
        this.aeroport = aeroport;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Boolean getAeroport() {
        return aeroport;
    }

    public void setAeroport(Boolean aeroport) {
        this.aeroport = aeroport;
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
                hotel.setCode(rs.getString("code"));
                hotel.setLibelle(rs.getString("libelle"));
                hotel.setAeroport(rs.getBoolean("aeroport"));
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
                hotel.setCode(rs.getString("code"));
                hotel.setLibelle(rs.getString("libelle"));
                hotel.setAeroport(rs.getBoolean("aeroport"));
                return hotel;
            }
        }
        return null;
    }
}
