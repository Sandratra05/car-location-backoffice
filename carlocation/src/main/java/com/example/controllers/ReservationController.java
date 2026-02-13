package com.example.controllers;

import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.annotations.JsonResponse;
import mg.ririnina.annotations.PostMapping;
import mg.ririnina.annotations.RequestParam;
import mg.ririnina.view.ModelView;

import com.example.entity.Reservation;
import com.example.entity.Hotel;
import com.example.service.TokenService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ReservationController {
    
    private final TokenService tokenService;

    public ReservationController() {
        this.tokenService = new TokenService();
    }

    @GetMapping("/reservations/new")
    public ModelView newReservation() {
        ModelView mv = new ModelView();
        try {
            mv.setView("/reservations-form.jsp");
            mv.addAttribute("hotels", Hotel.findAll());
            mv.addAttribute("action", "create");
            mv.addAttribute("title", "Nouvelle réservation");
        } catch (SQLException e) {
            mv.setView("/reservations-form.jsp");
            mv.addAttribute("error", "Impossible de charger les hôtels : " + e.getMessage());
        }
        return mv;
    }

    @PostMapping("/reservations/create")
    public ModelView createReservation(
            @RequestParam("nbPassager") String nbPassager,
            @RequestParam("dateHeureArrivee") String dateHeureArrivee,
            @RequestParam("idHotel") String idHotel,
            @RequestParam("idClient") String idClient) {

        ModelView mv = new ModelView();

        try {
            Reservation reservation = new Reservation();

            reservation.setNbPassager(Integer.parseInt(nbPassager));
            reservation.setDateHeureArrivee(Timestamp.valueOf(dateHeureArrivee.replace("T", " ") + ":00"));

            Hotel hotel = Hotel.findById(Integer.parseInt(idHotel));
            reservation.setHotel(hotel);

            reservation.setIdClient(idClient);

            reservation.save();

            mv.setView("/reservations-form.jsp");
            mv.addAttribute("success", "Réservation enregistrée avec succès !");
            mv.addAttribute("hotels", Hotel.findAll());

        } catch (Exception e) {
            mv.setView("/reservations-form.jsp");
            mv.addAttribute("error", "Erreur lors de l'enregistrement" + e.getMessage());
            mv.addAttribute("action", "create");
        }

        return mv;
    }

    /**
     * GET /api/reservations - Liste toutes les réservations (protégé)
     */
    @GetMapping("/api/reservations")
    @JsonResponse
    public Map<String, Object> getAllReservations(@RequestParam("token") String token) {
        try {
            if (!isTokenValid(token)) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "Token invalide ou expiré");
                error.put("code", 401);
                error.put("data", null);
                return error;
            }
            
            List<Reservation> reservations = Reservation.findAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Réservations récupérées avec succès");
            response.put("data", reservations);
            return response;
            
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Erreur lors de la récupération des réservations: " + e.getMessage());
            error.put("code", 500);
            error.put("data", null);
            return error;
        }
    }

    // ========== HELPERS ==========

    private boolean isTokenValid(String token) {
        try {
            return tokenService.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}