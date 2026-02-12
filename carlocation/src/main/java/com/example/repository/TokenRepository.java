package com.example.repository;

import com.example.config.DbConnection;
import com.example.entity.Token;

import java.sql.*;
import java.time.LocalDateTime;

public class TokenRepository {

    
    public Token save(Token token) throws SQLException {
        String sql = "INSERT INTO token (token, date_expiration) VALUES (?, ?) RETURNING id";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token.getToken());
            stmt.setTimestamp(2, Timestamp.valueOf(token.getDateExpiration()));
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                token.setId(rs.getLong("id"));
            }
            return token;
        }
    }

    
    public Token findByToken(String tokenValue) throws SQLException {
        String sql = "SELECT * FROM token WHERE token = ?";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, tokenValue);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return new Token(
                    rs.getLong("id"),
                    rs.getString("token"),
                    rs.getTimestamp("date_expiration").toLocalDateTime()
                );
            }
            return null;
        }
    }

    
    public void deleteExpiredTokens() throws SQLException {
        String sql = "DELETE FROM token WHERE date_expiration < ?";
        
        try (Connection conn = DbConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
        }
    }
}