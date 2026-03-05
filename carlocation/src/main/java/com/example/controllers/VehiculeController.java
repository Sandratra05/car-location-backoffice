package com.example.controllers;

import com.example.entity.Vehicule;
import com.example.enums.TypeCarburant;
import com.example.service.VehiculeService;
import mg.ririnina.annotations.*;
import mg.ririnina.view.ModelView;

import java.util.List;

@Controller
public class VehiculeController {

    private final VehiculeService vehiculeService;

    public VehiculeController() {
        this.vehiculeService = new VehiculeService();
    }

    // ========== LISTE ==========

    /**
     * GET /vehicules - Page liste des véhicules
     */
    @GetMapping("/vehicules")
    public ModelView listVehicules() {
        ModelView mv = new ModelView();
        mv.setView("/vehicule/list.jsp");
        mv.addAttribute("title", "Liste des Véhicules");
        try {
            List<Vehicule> vehicules = vehiculeService.getAllVehicules();
            mv.addAttribute("vehicules", vehicules);
            mv.addAttribute("title", "Liste des Véhicules");
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors du chargement : " + e.getMessage());
        }
        return mv;
    }

    /**
     * GET /vehicules/new - Formulaire création
     */
    @GetMapping("/vehicules/new")
    public ModelView newVehiculeForm() {
        ModelView mv = new ModelView();
        mv.setView("/vehicule/form.jsp");
        mv.addAttribute("title", "Nouveau Véhicule");
        mv.addAttribute("vehicule", null);
        mv.addAttribute("typeCarburants", TypeCarburant.values());
        return mv;
    }

    /**
     * POST /vehicules/create - Enregistre un nouveau véhicule
     */
    @PostMapping("/vehicules/create")
    public ModelView createVehicule(
            @RequestParam("reference") String reference,
            @RequestParam("nbPlace") String nbPlace,
            @RequestParam("typeCarburant") String typeCarburant) {
        ModelView mv = new ModelView();
        try {
            int places = Integer.parseInt(nbPlace);
            TypeCarburant type = TypeCarburant.valueOf(typeCarburant.toUpperCase());

            Vehicule vehicule = new Vehicule();
            vehicule.setReference(reference);
            vehicule.setNbPlace(places);
            vehicule.setTypeCarburant(type);

            vehiculeService.createVehicule(vehicule);

            // Retourner la liste avec message succès
            List<Vehicule> vehicules = vehiculeService.getAllVehicules();
            mv.setView("/vehicule/list.jsp");
            mv.addAttribute("vehicules", vehicules);
            mv.addAttribute("title", "Liste des Véhicules");
            mv.addAttribute("success", "Véhicule créé avec succès !");
        } catch (NumberFormatException e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Nouveau Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", "Nombre de places invalide");
        } catch (IllegalArgumentException e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Nouveau Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", "Erreur de validation : " + e.getMessage());
        } catch (Exception e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Nouveau Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", "Erreur : " + e.getMessage());
        }
        return mv;
    }

    /**
     * GET /vehicules/edit?id={id} - Formulaire modification
     */
    @GetMapping("/vehicules/edit")
    public ModelView editVehiculeForm(@RequestParam("id") String id) {
        ModelView mv = new ModelView();
        mv.setView("/vehicule/form.jsp");
        mv.addAttribute("title", "Modifier le Véhicule");
        mv.addAttribute("typeCarburants", TypeCarburant.values());
        try {
            Long vehiculeId = Long.parseLong(id);
            Vehicule vehicule = vehiculeService.getVehiculeById(vehiculeId);
            mv.addAttribute("vehicule", vehicule);
        } catch (Exception e) {
            mv.addAttribute("error", "Véhicule introuvable : " + e.getMessage());
        }
        return mv;
    }

    /**
     * POST /vehicules/update - Enregistre la modification
     */
    @PostMapping("/vehicules/update")
    public ModelView updateVehicule(
            @RequestParam("id") String id,
            @RequestParam("reference") String reference,
            @RequestParam("nbPlace") String nbPlace,
            @RequestParam("typeCarburant") String typeCarburant) {
        ModelView mv = new ModelView();
        try {
            Long vehiculeId = Long.parseLong(id);
            int places = Integer.parseInt(nbPlace);
            TypeCarburant type = TypeCarburant.valueOf(typeCarburant.toUpperCase());

            Vehicule vehicule = new Vehicule();
            vehicule.setReference(reference);
            vehicule.setNbPlace(places);
            vehicule.setTypeCarburant(type);

            vehiculeService.updateVehicule(vehiculeId, vehicule);

            List<Vehicule> vehicules = vehiculeService.getAllVehicules();
            mv.setView("/vehicule/list.jsp");
            mv.addAttribute("vehicules", vehicules);
            mv.addAttribute("title", "Liste des Véhicules");
            mv.addAttribute("success", "Véhicule mis à jour avec succès !");
        } catch (NumberFormatException e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Modifier le Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", "ID ou nombre de places invalide");
        } catch (IllegalArgumentException e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Modifier le Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            mv.setView("/vehicule/form.jsp");
            mv.addAttribute("title", "Modifier le Véhicule");
            mv.addAttribute("typeCarburants", TypeCarburant.values());
            mv.addAttribute("error", "Erreur : " + e.getMessage());
        }
        return mv;
    }

    /**
     * GET /vehicules/delete?id={id} - Supprime un véhicule
     */
    @GetMapping("/vehicules/delete")
    public ModelView deleteVehicule(@RequestParam("id") String id) {
        ModelView mv = new ModelView();
        mv.setView("/vehicule/list.jsp");
        mv.addAttribute("title", "Liste des Véhicules");
        try {
            Long vehiculeId = Long.parseLong(id);
            vehiculeService.deleteVehicule(vehiculeId);

            List<Vehicule> vehicules = vehiculeService.getAllVehicules();
            mv.addAttribute("vehicules", vehicules);
            mv.addAttribute("success", "Véhicule supprimé avec succès !");
        } catch (IllegalArgumentException e) {
            mv.addAttribute("error", e.getMessage());
        } catch (Exception e) {
            mv.addAttribute("error", "Erreur lors de la suppression : " + e.getMessage());
        }
        return mv;
    }
}

