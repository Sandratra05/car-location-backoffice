package com.example.service;

import com.example.entity.Token;
import com.example.repository.TokenRepository;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class TokenService {
    
    private final TokenRepository tokenRepository;

    public TokenService() {
        this.tokenRepository = new TokenRepository();
    }

    /**
     * Génère un nouveau token UUID avec une date d'expiration
     * @param dateExpiration Date d'expiration à définir manuellement
     */
    public Token generateToken(LocalDateTime dateExpiration) throws SQLException {
        String tokenValue = UUID.randomUUID().toString();
        
        Token token = new Token(tokenValue, dateExpiration);
        return tokenRepository.save(token);
    }

    /**
     * Valide un token : vérifie existence et expiration
     */
    public boolean validateToken(String tokenValue) throws SQLException {
        if (tokenValue == null || tokenValue.isEmpty()) {
            return false;
        }

        Token token = tokenRepository.findByToken(tokenValue);
        
        if (token == null) {
            return false; // Token inexistant
        }

        if (token.isExpired()) {
            return false; // Token expiré
        }

        return true;
    }

    /**
     * Récupère un token par sa valeur
     */
    public Token getTokenByValue(String tokenValue) throws SQLException {
        return tokenRepository.findByToken(tokenValue);
    }

    /**
     * Nettoie les tokens expirés (à appeler périodiquement)
     */
    public void cleanExpiredTokens() throws SQLException {
        tokenRepository.deleteExpiredTokens();
    }
}