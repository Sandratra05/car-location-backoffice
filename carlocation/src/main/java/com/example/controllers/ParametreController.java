package com.example.controllers;

import com.example.entity.Parametre;
import com.example.repository.ParametreRepository;
import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.annotations.PostMapping;
import mg.ririnina.annotations.RequestParam;
import mg.ririnina.view.ModelView;

import java.math.BigDecimal;

@Controller
public class ParametreController {

    private final ParametreRepository parametreRepository;

    public ParametreController() {
        this.parametreRepository = new ParametreRepository();
    }

    @GetMapping("/parametres/new")
    public ModelView newParametre() {
        ModelView mv = new ModelView();
        try {
            mv.setView("/parametre-form.jsp");
            Parametre latest = parametreRepository.findLatest();
            if (latest != null) mv.addAttribute("parametre", latest);
            mv.addAttribute("title", "Nouveau paramètre");
        } catch (Exception e) {
            mv.setView("/parametre-form.jsp");
            mv.addAttribute("error", "Impossible de charger le formulaire: " + e.getMessage());
        }
        return mv;
    }

    @PostMapping("/parametres/create")
    public ModelView createParametre(
            @RequestParam("vitesseMoyenneKmh") String vitesseStr,
            @RequestParam("tempsAttenteMin") String tempsStr) {

        ModelView mv = new ModelView();

        try {
            Parametre p = new Parametre();
            p.setVitesseMoyenneKmh(new BigDecimal(vitesseStr));
            p.setTempsAttenteMin(Integer.parseInt(tempsStr));

            parametreRepository.save(p);

            mv.setView("/parametre-form.jsp");
            mv.addAttribute("success", "Paramètre enregistré avec succès !");
            mv.addAttribute("parametre", p);
            mv.addAttribute("title", "Nouveau paramètre");

        } catch (Exception e) {
            mv.setView("/parametre-form.jsp");
            mv.addAttribute("error", "Erreur lors de l'enregistrement: " + e.getMessage());
        }

        return mv;
    }
}
