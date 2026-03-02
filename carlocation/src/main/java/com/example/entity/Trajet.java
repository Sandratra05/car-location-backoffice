package com.example.entity;

import java.sql.Timestamp;
import java.util.List;

public class Trajet {
    private Vehicule vehicule;
    private Hotel hotel;
    private List<Reservation> reservations;
    private Timestamp heureDepart;
    private Timestamp heureRetour;

    public Trajet() {}

    public Trajet(Vehicule vehicule, Hotel hotel, List<Reservation> reservations, Timestamp heureDepart, Timestamp heureRetour) {
        this.vehicule = vehicule;
        this.hotel = hotel;
        this.reservations = reservations;
        this.heureDepart = heureDepart;
        this.heureRetour = heureRetour;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Timestamp getHeureDepart() {
        return heureDepart;
    }

    public void setHeureDepart(Timestamp heureDepart) {
        this.heureDepart = heureDepart;
    }

    public Timestamp getHeureRetour() {
        return heureRetour;
    }

    public void setHeureRetour(Timestamp heureRetour) {
        this.heureRetour = heureRetour;
    }
}
