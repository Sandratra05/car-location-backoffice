package com.example.controllers;

import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.annotations.JsonResponse;
import mg.ririnina.annotations.PostMapping;
import mg.ririnina.annotations.RequestParam;
import mg.ririnina.view.ModelView;

import com.example.entity.Reservation;
import com.example.entity.Hotel;
import com.example.entity.Vehicule;
import com.example.service.VehiculeService;
import com.example.service.TokenService;
import com.example.repository.ParametreRepository;
import com.example.entity.Parametre;

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

    @GetMapping("/planning/new")
    public ModelView newPlanning() {
        ModelView mv = new ModelView();
        mv.setView("planning-form.jsp");
        return mv;
    }

    @PostMapping("/planning/planify")
    public ModelView planify(@RequestParam("date") String date) {
        ModelView mv = new ModelView();
        try {
            Timestamp ts = Timestamp.valueOf(date.replace("T", " ") + ":00");
            VehiculeService vs = new VehiculeService();
            // planifyByDate gère : récup des réservations + découpage en intervalles + assignations
            Map<Vehicule, List<Reservation>> assignments = vs.planifyByDate(ts);
            // Heure de départ affichée (fallback simple)
            Timestamp heureDepart = Reservation.getHeureDepartAvecTA(ts);
            if (heureDepart == null) heureDepart = ts;

            // Récupérer le temps d'attente
            ParametreRepository paramRepo = new ParametreRepository();
            Parametre p = paramRepo.findLatest();
            Integer tempsAttenteMin = (p != null && p.getTempsAttenteMin() != null) ? p.getTempsAttenteMin() : 30; // default 30 min

            // Préparer les données supplémentaires pour l'affichage
            Map<Vehicule, String> routes = new HashMap<>();
            Map<Vehicule, Timestamp> departTimes = new HashMap<>();
            Map<Vehicule, Timestamp> returnTimes = new HashMap<>();
            Map<Vehicule, java.math.BigDecimal> kmMap = new HashMap<>();
            Map<Vehicule, Integer> trajetsMap = new HashMap<>();
            Map<String, Integer> trajetsSummary = new HashMap<>();
            Map<Vehicule, String> placesMap = new HashMap<>();
            Map<Vehicule, Integer> occupiedMap = new HashMap<>();

            Map<Vehicule, Timestamp> intervalDepartTimes = vs.getLastDepartTimes();

            for (Map.Entry<Vehicule, List<Reservation>> entry : assignments.entrySet()) {
                Vehicule v = entry.getKey();
                List<Reservation> resas = entry.getValue();

                try {
                    String route = vs.getRouteDescription(resas);
                    routes.put(v, route);
                } catch (Exception e) {
                    routes.put(v, "Aéroport -> Aéroport");
                }

                try {
                    java.math.BigDecimal km = vs.calculTotalDistance(resas);
                    kmMap.put(v, km);
                } catch (Exception e) { 
                    kmMap.put(v, java.math.BigDecimal.ZERO);
                }

                // Nb trajets
                try {
                    int nbTrajets = vs.countTrajets(v.getId());
                    trajetsMap.put(v, nbTrajets);
                    trajetsSummary.put(v.getReference(), nbTrajets);
                } catch (Exception e) {
                    trajetsMap.put(v, 0);
                    trajetsSummary.put(v.getReference(), 0);
                }

                // Places occupées / total
                int occupied = 0;
                for (Reservation r : resas) {
                    if (r.getNbPassager() != null) {
                        occupied += r.getNbPassager();
                    }
                }
                placesMap.put(v, occupied + "/" + v.getNbPlace());
                occupiedMap.put(v, occupied);

                // Heure de départ = heure de départ de l'intervalle (commune à tous les véhicules de l'intervalle)
                Timestamp departInterval = intervalDepartTimes.get(v);
                departTimes.put(v, departInterval != null ? departInterval : heureDepart);

                Timestamp ret = null;
                try {
                    if (!resas.isEmpty()) {
                        ret = resas.get(0).calculHeureRetourTotal(resas);
                    }
                } catch (Exception e) {}
                returnTimes.put(v, ret);
            }

            // Récupérer les réservations non assignées
            List<Reservation> allReservations = Reservation.findReservationsByDateASC(ts);
            List<Reservation> unassigned = vs.findUnassignedReservations(allReservations, assignments);

            mv.setView("planning-result.jsp");
            mv.addAttribute("assignments", assignments);
            mv.addAttribute("routes", routes);
            mv.addAttribute("departTimes", departTimes);
            mv.addAttribute("returnTimes", returnTimes);
            mv.addAttribute("kmMap", kmMap);
            mv.addAttribute("trajetsMap", trajetsMap);
            mv.addAttribute("trajetsSummary", trajetsSummary);
            mv.addAttribute("placesMap", placesMap);
            mv.addAttribute("occupiedMap", occupiedMap);
            mv.addAttribute("tempsAttenteMin", tempsAttenteMin);
            mv.addAttribute("unassigned", unassigned);
        } catch (Exception e) {
            mv.setView("planning-form.jsp");
            mv.addAttribute("error", "Erreur lors de la génération du planning: " + e.getMessage());
        }
        return mv;
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