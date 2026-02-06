package com.example.entity;

public class Hotel {
    private Integer idHotel;
    private String libelle;
    private Double distance;

    public Hotel() {
    }

    public Hotel(Integer idHotel, String libelle, Double distance) {
        this.idHotel = idHotel;
        this.libelle = libelle;
        this.distance = distance;
    }

    public Integer getIdHotel() {
        return idHotel;
    }

    public void setIdHotel(Integer idHotel) {
        this.idHotel = idHotel;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

}
