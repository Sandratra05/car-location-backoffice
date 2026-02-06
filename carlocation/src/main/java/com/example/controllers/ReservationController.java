package com.example.controllers;

import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.annotations.JsonResponse;
import mg.ririnina.annotations.PostMapping;
import mg.ririnina.annotations.RequestParam;
import mg.ririnina.view.ModelView;

import com.example.entity.Reservation;
import com.example.entity.Hotel;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Controller
public class ReservationController {

    @GetMapping("/reservations/new")
    public ModelView newReservation() {
        ModelView mv = new ModelView();
        try {
            mv.setView("/reservations-form.jsp");
            mv.addAttribute("hotels", Hotel.findAll());
            mv.addAttribute("action", "create");
            mv.addAttribute("title", "Nouvelle réservation");
        } catch (SQLException e) {
            mv.setView("/error.jsp");
            mv.addAttribute("error", e.getMessage());
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

            mv.setView("/reservations-list.jsp");
            mv.addAttribute("reservations", Reservation.findAll());
            mv.addAttribute("hotels", Hotel.findAll());
            mv.addAttribute("message", "Liste des réservations");

        } catch (Exception e) {
            mv.setView("/reservations-form.jsp");
            mv.addAttribute("error", e.getMessage());
            mv.addAttribute("action", "create");
        }

        return mv;
    }

    @JsonResponse
    @GetMapping("/api/reservations")
    public List<Reservation> getAllReservations() throws SQLException {
        return Reservation.findAll();
    }

}
