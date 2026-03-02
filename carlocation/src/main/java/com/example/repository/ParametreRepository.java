package com.example.repository;

import com.example.config.DbConnection;
import com.example.entity.Parametre;

import java.math.BigDecimal;
import java.sql.*;

public class ParametreRepository {

    public Parametre save(Parametre parametre) throws SQLException {
        String sql = "INSERT INTO parametre (vitesse_moyenne_kmh, temps_attente_min) VALUES (?, ?) RETURNING id_parametre";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, parametre.getVitesseMoyenneKmh());
            stmt.setInt(2, parametre.getTempsAttenteMin());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                parametre.setIdParametre(rs.getLong("id_parametre"));
            }
            return parametre;
        }
    }

    public Parametre findLatest() throws SQLException {
        String sql = "SELECT * FROM parametre ORDER BY id_parametre DESC LIMIT 1";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                Parametre p = new Parametre();
                p.setIdParametre(rs.getLong("id_parametre"));
                p.setVitesseMoyenneKmh(rs.getBigDecimal("vitesse_moyenne_kmh"));
                p.setTempsAttenteMin(rs.getInt("temps_attente_min"));
                return p;
            }
            return null;
        }
    }
}
