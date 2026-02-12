package com.example.entity;

import java.time.LocalDateTime;

public class Token {
    private Long id;
    private String token;
    private LocalDateTime dateExpiration;

    
    public Token() {}

    public Token(String token, LocalDateTime dateExpiration) {
        this.token = token;
        this.dateExpiration = dateExpiration;
    }

    public Token(Long id, String token, LocalDateTime dateExpiration) {
        this.id = id;
        this.token = token;
        this.dateExpiration = dateExpiration;
    }

    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(dateExpiration);
    }

   
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getDateExpiration() {
        return dateExpiration;
    }

    public void setDateExpiration(LocalDateTime dateExpiration) {
        this.dateExpiration = dateExpiration;
    }
}