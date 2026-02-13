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
     * POST /api/token/generate - Génère un nouveau token
     */
    @PostMapping("/api/token/generate")
    @JsonResponse
    public Map<String, Object> generateToken(@RequestParam("dateExpiration") String dateExpirationStr) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateExpiration = LocalDateTime.parse(dateExpirationStr, formatter);
            
            Token token = tokenService.generateToken(dateExpiration);
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token.getToken());
            data.put("expiresAt", token.getDateExpiration().toString());
            
            return createResponse(true, "Token généré avec succès", data);
        } catch (Exception e) {
            return createResponse(false, "Erreur lors de la génération du token: " + e.getMessage(), null);
        }
    }

    /**
     * GET /api/token/validate - Valide un token via paramètre
     */
    @GetMapping("/api/token/validate")
    @JsonResponse
    public Map<String, Object> validateToken(@RequestParam("token") String token) {
        try {
            boolean isValid = tokenService.validateToken(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("valid", isValid);
            data.put("token", token);
            
            String message = isValid ? "Token valide" : "Token invalide ou expiré";
            return createResponse(isValid, message, data);
        } catch (Exception e) {
            return createResponse(false, "Erreur lors de la validation: " + e.getMessage(), null);
        }
    }

    // ========== HELPERS ==========

    private Map<String, Object> createResponse(boolean success, String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }
}