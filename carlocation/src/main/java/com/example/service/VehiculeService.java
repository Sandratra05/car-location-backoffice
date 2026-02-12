package com.example.service;

import com.example.entity.Vehicule;
import com.example.repository.VehiculeRepository;

import java.sql.SQLException;
import java.util.List;

public class VehiculeService {
    
    private final VehiculeRepository vehiculeRepository;

    public VehiculeService() {
        this.vehiculeRepository = new VehiculeRepository();
    }

    public Vehicule createVehicule(Vehicule vehicule) throws SQLException {
        // Validation
        if (vehicule.getReference() == null || vehicule.getReference().isEmpty()) {
            throw new IllegalArgumentException("La référence est obligatoire");
        }
        if (vehicule.getNbPlace() <= 0) {
            throw new IllegalArgumentException("Le nombre de places doit être positif");
        }
        if (vehicule.getTypeCarburant() == null) {
            throw new IllegalArgumentException("Le type de carburant est obligatoire");
        }
        
        return vehiculeRepository.save(vehicule);
    }

    public List<Vehicule> getAllVehicules() throws SQLException {
        return vehiculeRepository.findAll();
    }

    public Vehicule getVehiculeById(Long id) throws SQLException {
        Vehicule vehicule = vehiculeRepository.findById(id);
        if (vehicule == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        return vehicule;
    }

    public Vehicule updateVehicule(Long id, Vehicule vehicule) throws SQLException {
        // Vérifier que le véhicule existe
        Vehicule existing = vehiculeRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        
        vehicule.setId(id);
        return vehiculeRepository.update(vehicule);
    }

    public void deleteVehicule(Long id) throws SQLException {
        Vehicule existing = vehiculeRepository.findById(id);
        if (existing == null) {
            throw new IllegalArgumentException("Véhicule non trouvé avec l'id: " + id);
        }
        vehiculeRepository.deleteById(id);
    }
}