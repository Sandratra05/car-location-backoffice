package com.example.repository;

import com.example.config.DbConnection;
import com.example.entity.Distance;

import java.sql.*;

public class DistanceRepository {

    public Distance save(Distance distance) throws SQLException {
        String sql = "INSERT INTO distance (from_hotel_id, to_hotel_id, kilometre) VALUES (?, ?, ?) RETURNING id_distance";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, distance.getFrom());
            stmt.setInt(2, distance.getTo());
            stmt.setBigDecimal(3, distance.getKilometre());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                distance.setIdDistance(rs.getLong("id_distance"));
            }
            return distance;
        }
    }

    public Distance findDistance(int fromHotelId, int toHotelId) throws SQLException {
        String sql = "SELECT * FROM distance WHERE from_hotel_id = ? AND to_hotel_id = ? LIMIT 1";

        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, fromHotelId);
            stmt.setInt(2, toHotelId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Distance d = new Distance();
                    d.setIdDistance(rs.getLong("id_distance"));
                    d.setFrom(rs.getInt("from_hotel_id"));
                    d.setTo(rs.getInt("to_hotel_id"));
                    d.setKilometre(rs.getBigDecimal("kilometre"));
                    return d;
                }
            }
            return null;
        }
    }
}
