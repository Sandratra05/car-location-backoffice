package com.example.controllers;

import com.example.dto.VehiculeDTO;
import com.example.entity.Vehicule;
import com.example.enums.TypeCarburant;
import com.example.service.VehiculeService;
import com.example.service.TokenService;
import mg.ririnina.annotations.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class VehiculeController {
    
    private final VehiculeService vehiculeService;
    private final TokenService tokenService;

    public VehiculeController() {
        this.vehiculeService = new VehiculeService();
        this.tokenService = new TokenService();
    }

    /**
     * GET /api/vehicules - Liste tous les véhicules
     */
    @GetMapping("/api/vehicules")
    @JsonResponse(status = "success", code = 200)
    public Map<String, Object> getAllVehicules(@Session Map<String, Object> session) {
        try {
            if (!isTokenValid(session)) {
                return createErrorResponse("Token invalide ou expiré", 401);
            }
            
            List<Vehicule> vehicules = vehiculeService.getAllVehicules();
            List<VehiculeDTO> vehiculeDTOs = vehicules.stream()
                .map(VehiculeDTO::new)
                .collect(Collectors.toList());
            
            return createSuccessResponse(vehiculeDTOs);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Erreur: " + e.getMessage(), 500);
        }
    }

    /**
     * GET /api/vehicules/{id} - Récupère un véhicule par ID
     */
    @GetMapping("/api/vehicules/{id}")
    @JsonResponse(status = "success", code = 200)
    public Map<String, Object> getVehiculeById(String id, @Session Map<String, Object> session) {
        try {
            if (!isTokenValid(session)) {
                return createErrorResponse("Token invalide ou expiré", 401);
            }
            
            Long vehiculeId = Long.parseLong(id);
            Vehicule vehicule = vehiculeService.getVehiculeById(vehiculeId);
            VehiculeDTO dto = new VehiculeDTO(vehicule);
            
            return createSuccessResponse(dto);
        } catch (NumberFormatException e) {
            return createErrorResponse("ID invalide: " + id, 400);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Erreur: " + e.getMessage(), 500);
        }
    }

    /**
     * POST /api/vehicules/create - Crée un nouveau véhicule
     */
    @PostMapping("/api/vehicules/create")
    @JsonResponse(status = "success", code = 201)
    public Map<String, Object> createVehicule(
            @RequestParam("reference") String reference,
            @RequestParam("nbPlace") int nbPlace,
            @RequestParam("typeCarburant") String typeCarburantStr,
            @Session Map<String, Object> session) {
        try {
            if (!isTokenValid(session)) {
                return createErrorResponse("Token invalide ou expiré", 401);
            }
            
            TypeCarburant typeCarburant = TypeCarburant.valueOf(typeCarburantStr.toUpperCase());
            
            Vehicule vehicule = new Vehicule();
            vehicule.setReference(reference);
            vehicule.setNbPlace(nbPlace);
            vehicule.setTypeCarburant(typeCarburant);

            Vehicule created = vehiculeService.createVehicule(vehicule);
            VehiculeDTO dto = new VehiculeDTO(created);
            
            return createSuccessResponse(dto, "Véhicule créé avec succès");
        } catch (IllegalArgumentException e) {
            return createErrorResponse("Validation: " + e.getMessage(), 400);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Erreur: " + e.getMessage(), 500);
        }
    }

 /**
 * POST /api/vehicules/{id}/update - Met à jour un véhicule
 */
@PostMapping("/api/vehicules/{id}/update")
@JsonResponse(status = "success", code = 200)
public Map<String, Object> updateVehicule(
        @RequestParam("reference") String reference,
        @RequestParam("nbPlace") String nbPlace,
        @RequestParam("typeCarburant") String typeCarburant,
        String id,  // ✅ Paramètre de path EN DERNIER
        @Session Map<String, Object> session) {
    try {
        System.out.println("========== updateVehicule ==========");
        System.out.println("ID reçu: " + id);
        System.out.println("Reference: " + reference);
        System.out.println("NbPlace: " + nbPlace);
        System.out.println("TypeCarburant: " + typeCarburant);
        System.out.println("======================================");
        
        if (!isTokenValid(session)) {
            return createErrorResponse("Token invalide ou expiré", 401);
        }
        
        if (id == null || id.isEmpty()) {
            return createErrorResponse("ID manquant dans l'URL", 400);
        }
        
        // Convertir les paramètres
        Long vehiculeId = Long.parseLong(id);
        int places = Integer.parseInt(nbPlace);
        TypeCarburant type = TypeCarburant.valueOf(typeCarburant.toUpperCase());
        
        Vehicule vehicule = new Vehicule();
        vehicule.setReference(reference);
        vehicule.setNbPlace(places);
        vehicule.setTypeCarburant(type);

        Vehicule updated = vehiculeService.updateVehicule(vehiculeId, vehicule);
        VehiculeDTO dto = new VehiculeDTO(updated);
        
        return createSuccessResponse(dto, "Véhicule mis à jour avec succès");
    } catch (NumberFormatException e) {
        return createErrorResponse("ID ou nombre de places invalide", 400);
    } catch (IllegalArgumentException e) {
        return createErrorResponse(e.getMessage(), 404);
    } catch (Exception e) {
        e.printStackTrace();
        return createErrorResponse("Erreur: " + e.getMessage(), 500);
    }
}

    /**
     * GET /api/vehicules/{id}/delete - Supprime un véhicule
     */
    @GetMapping("/api/vehicules/{id}/delete")
    @JsonResponse(status = "success", code = 200)
    public Map<String, Object> deleteVehicule(String id, @Session Map<String, Object> session) {
        try {
            if (!isTokenValid(session)) {
                return createErrorResponse("Token invalide ou expiré", 401);
            }
            
            Long vehiculeId = Long.parseLong(id);
            vehiculeService.deleteVehicule(vehiculeId);
            
            return createSuccessResponse(null, "Véhicule supprimé avec succès");
        } catch (NumberFormatException e) {
            return createErrorResponse("ID invalide: " + id, 400);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(e.getMessage(), 404);
        } catch (Exception e) {
            e.printStackTrace();
            return createErrorResponse("Erreur: " + e.getMessage(), 500);
        }
    }

    // ========== HELPERS ==========

    private boolean isTokenValid(Map<String, Object> session) {
        if (session == null) return false;
        
        String token = (String) session.get("auth_token");
        if (token == null || token.isEmpty()) return false;
        
        try {
            return tokenService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Map<String, Object> createSuccessResponse(Object data) {
        return createSuccessResponse(data, "Succès");
    }

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> createErrorResponse(String message, int code) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("code", code);
        response.put("data", null);
        return response;
    }
}