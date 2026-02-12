package com.example.repository;

import com.example.config.DbConnection;
import com.example.entity.Vehicule;
import com.example.enums.TypeCarburant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehiculeRepository {

   
    public Vehicule save(Vehicule vehicule) throws SQLException {
        String sql = "INSERT INTO vehicule (reference, nb_place, type_carburant) VALUES (?, ?, ?::type_carburant) RETURNING id";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, vehicule.getReference());
            stmt.setInt(2, vehicule.getNbPlace());
            stmt.setString(3, vehicule.getTypeCarburant().name());
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                vehicule.setId(rs.getLong("id"));
            }
            return vehicule;
        }
    }

    
    public List<Vehicule> findAll() throws SQLException {
        List<Vehicule> vehicules = new ArrayList<>();
        String sql = "SELECT * FROM vehicule ORDER BY id";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                vehicules.add(mapResultSetToVehicule(rs));
            }
        }
        return vehicules;
    }

    
    public Vehicule findById(Long id) throws SQLException {
        String sql = "SELECT * FROM vehicule WHERE id = ?";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToVehicule(rs);
            }
            return null;
        }
    }

   
    public Vehicule update(Vehicule vehicule) throws SQLException {
        String sql = "UPDATE vehicule SET reference = ?, nb_place = ?, type_carburant = ?::type_carburant WHERE id = ?";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, vehicule.getReference());
            stmt.setInt(2, vehicule.getNbPlace());
            stmt.setString(3, vehicule.getTypeCarburant().name());
            stmt.setLong(4, vehicule.getId());
            
            stmt.executeUpdate();
            return vehicule;
        }
    }

   
    public void deleteById(Long id) throws SQLException {
        String sql = "DELETE FROM vehicule WHERE id = ?";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    // Helper method
    private Vehicule mapResultSetToVehicule(ResultSet rs) throws SQLException {
        return new Vehicule(
            rs.getLong("id"),
            rs.getString("reference"),
            rs.getInt("nb_place"),
            TypeCarburant.valueOf(rs.getString("type_carburant"))
        );
    }
}