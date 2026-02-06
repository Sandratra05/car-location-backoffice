package com.example.controllers;

import com.example.config.DbConnection;
import mg.ririnina.annotations.Controller;
import mg.ririnina.annotations.GetMapping;
import mg.ririnina.view.ModelView;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TestController {
    @GetMapping("/test")
    public ModelView test() {
        ModelView mv = new ModelView();
        mv.setView("test.jsp");
        mv.addAttribute("message", "Le framework ririnina fonctionne !");
        return mv;
    }

    @GetMapping("/hello")
    public ModelView hello() {
        ModelView mv = new ModelView();
        mv.setView("hello.jsp");
        mv.addAttribute("greeting", "Bonjour depuis le contrôleur TestController !");
        return mv;
    }

    @GetMapping("/db")
    public ModelView dbTest() {
        String query = "SELECT * FROM test";
        try (Connection connection = DbConnection.getInstance().getConnection()) {
            PreparedStatement prepStmt = connection.prepareStatement(query);
            prepStmt.executeQuery();
            List<String> messages = new ArrayList<>();
            while (prepStmt.getResultSet().next()) {
                String dbMessage = prepStmt.getResultSet().getString("name");
                messages.add(dbMessage);
            }
            ModelView mv = new ModelView();
            mv.setView("dbTest.jsp");
            mv.addAttribute("dbMessages", messages);
            return mv;
        } catch (Exception e) {
            ModelView mv = new ModelView();
            mv.setView("dbTest.jsp");
            mv.addAttribute("dbMessage", "Échec de la connexion à la base de données : " + e.getMessage());
            return mv;
        }
    }
//        ModelView mv = new ModelView();
//        mv.setView("dbTest.jsp");
//        mv.addAttribute("dbMessage", "Connexion à la base de données réussie !");
//        return mv;
//    }
}