package com.example.dto;

import com.example.entity.Vehicule;
// import com.example.enums.TypeCarburant;

public class VehiculeDTO {
    private Long id;
    private String reference;
    private int nbPlace;
    private String typeCarburant;

    // Constructeur vide
    public VehiculeDTO() {}

    // Constructeur depuis une entité
    public VehiculeDTO(Vehicule vehicule) {
        this.id = vehicule.getId();
        this.reference = vehicule.getReference();
        this.nbPlace = vehicule.getNbPlace();
        this.typeCarburant = vehicule.getTypeCarburant() != null 
            ? vehicule.getTypeCarburant().name() 
            : null;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public int getNbPlace() {
        return nbPlace;
    }

    public void setNbPlace(int nbPlace) {
        this.nbPlace = nbPlace;
    }

    public String getTypeCarburant() {
        return typeCarburant;
    }

    public void setTypeCarburant(String typeCarburant) {
        this.typeCarburant = typeCarburant;
    }
}