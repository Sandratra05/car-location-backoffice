package com.example.entity;

import com.example.enums.TypeCarburant;

public class Vehicule {
    private Long id;
    private String reference;
    private int nbPlace;
    private TypeCarburant typeCarburant;

  
    public Vehicule() {}

    
    public Vehicule(Long id, String reference, int nbPlace, TypeCarburant typeCarburant) {
        this.id = id;
        this.reference = reference;
        this.nbPlace = nbPlace;
        this.typeCarburant = typeCarburant;
    }

  
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

    public TypeCarburant getTypeCarburant() {
        return typeCarburant;
    }

    public void setTypeCarburant(TypeCarburant typeCarburant) {
        this.typeCarburant = typeCarburant;
    }
}