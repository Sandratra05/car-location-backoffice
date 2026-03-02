package com.example.entity;

import java.math.BigDecimal;

public class Parametre {
    private Long idParametre;
    private BigDecimal vitesseMoyenneKmh;
    private Integer tempsAttenteMin;

    public Parametre() {}

    public Parametre(BigDecimal vitesseMoyenneKmh, Integer tempsAttenteMin) {
        this.vitesseMoyenneKmh = vitesseMoyenneKmh;
        this.tempsAttenteMin = tempsAttenteMin;
    }

    public Parametre(Long idParametre, BigDecimal vitesseMoyenneKmh, Integer tempsAttenteMin) {
        this.idParametre = idParametre;
        this.vitesseMoyenneKmh = vitesseMoyenneKmh;
        this.tempsAttenteMin = tempsAttenteMin;
    }

    public Long getIdParametre() {
        return idParametre;
    }

    public void setIdParametre(Long idParametre) {
        this.idParametre = idParametre;
    }

    public BigDecimal getVitesseMoyenneKmh() {
        return vitesseMoyenneKmh;
    }

    public void setVitesseMoyenneKmh(BigDecimal vitesseMoyenneKmh) {
        this.vitesseMoyenneKmh = vitesseMoyenneKmh;
    }

    public Integer getTempsAttenteMin() {
        return tempsAttenteMin;
    }

    public void setTempsAttenteMin(Integer tempsAttenteMin) {
        this.tempsAttenteMin = tempsAttenteMin;
    }
}
