package com.example.controllers;

import com.example.entity.Token;
import com.example.service.TokenService;
import mg.ririnina.annotations.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Controller
public class TokenController {
    
    private final TokenService tokenService;

    public TokenController() {
        this.tokenService = new TokenService();
    }

    /**
     * POST /api/token/generate - Génère un nouveau token avec date d'expiration
     * Paramètres :
     *   - dateExpiration (format: yyyy-MM-dd HH:mm:ss ou yyyy-MM-ddTHH:mm:ss)
     *   OU
     *   - hoursValid (nombre d'heures de validité, par défaut 24h)
     */
    @PostMapping("/api/token/generate")
    @JsonResponse(status = "success", code = 201)
    public Map<String, Object> generateToken(
            @RequestParam(value = "dateExpiration") String dateExpirationStr,
            @Session Map<String, Object> session) {
        try {
            LocalDateTime dateExpiration;
            
            // Si une date est fournie, l'utiliser
            if (dateExpirationStr != null && !dateExpirationStr.isEmpty()) {
                try {
                    // Essayer format ISO (yyyy-MM-ddTHH:mm:ss)
                    dateExpiration = LocalDateTime.parse(dateExpirationStr);
                } catch (Exception e1) {
                    try {
                        // Essayer format avec espace (yyyy-MM-dd HH:mm:ss)
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        dateExpiration = LocalDateTime.parse(dateExpirationStr, formatter);
                    } catch (Exception e2) {
                        return createResponse(false, "Format de date invalide. Utilisez: yyyy-MM-dd HH:mm:ss ou yyyy-MM-ddTHH:mm:ss", null);
                    }
                }
            } else {
                // Par défaut : 24h à partir de maintenant
                dateExpiration = LocalDateTime.now().plusHours(24);
            }
            
            Token token = tokenService.generateToken(dateExpiration);
            
            // Stocker le token dans la session
            session.put("auth_token", token.getToken());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token.getToken());
            data.put("expiresAt", token.getDateExpiration().toString());
            
            return createResponse(true, "Token généré avec succès", data);
            
        } catch (Exception e) {
            return createResponse(false, "Erreur lors de la génération du token: " + e.getMessage(), null);
        }
    }

    /**
     * POST /api/token/generate/hours - Génère un token avec validité en heures
     * Paramètre : hours (nombre d'heures de validité)
     */
    @PostMapping("/api/token/generate/hours")
    @JsonResponse(status = "success", code = 201)
    public Map<String, Object> generateTokenWithHours(
            @RequestParam(value = "hours") int hours,
            @Session Map<String, Object> session) {
        try {
            if (hours <= 0) {
                return createResponse(false, "Le nombre d'heures doit être positif", null);
            }
            
            LocalDateTime dateExpiration = LocalDateTime.now().plusHours(hours);
            Token token = tokenService.generateToken(dateExpiration);
            
            // Stocker le token dans la session
            session.put("auth_token", token.getToken());
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token.getToken());
            data.put("expiresAt", token.getDateExpiration().toString());
            data.put("validForHours", hours);
            
            return createResponse(true, "Token généré avec succès", data);
            
        } catch (Exception e) {
            return createResponse(false, "Erreur lors de la génération du token: " + e.getMessage(), null);
        }
    }

    /**
     * GET /api/token/validate - Vérifie si le token en session est valide
     */
    @GetMapping("/api/token/validate")
    @JsonResponse(status = "success", code = 200)
    public Map<String, Object> validateToken(@Session Map<String, Object> session) {
        try {
            String token = (String) session.get("auth_token");
            
            if (token == null) {
                return createResponse(false, "Aucun token en session", null);
            }
            
            boolean isValid = tokenService.validateToken(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            data.put("token", token);
            
            String message = isValid ? "Token valide" : "Token invalide ou expiré";
            return createResponse(isValid, message, data);
            
        } catch (Exception e) {
            return createResponse(false, "Erreur: " + e.getMessage(), null);
        }
    }

    /**
     * POST /api/token/logout - Supprime le token de la session
     */
    @PostMapping("/api/token/logout")
    @JsonResponse(status = "success", code = 200)
    @Authorized
    public Map<String, Object> logout(@Session Map<String, Object> session) {
        session.remove("auth_token");
        return createResponse(true, "Déconnexion réussie", null);
    }

    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}