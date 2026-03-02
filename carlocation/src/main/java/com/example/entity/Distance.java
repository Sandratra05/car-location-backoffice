package com.example.entity;

import java.math.BigDecimal;

public class Distance {
    private Long idDistance;
    private Integer from;
    private Integer to;
    private BigDecimal kilometre;

    public Distance() {}

    public Distance(Integer from, Integer to, BigDecimal kilometre) {
        this.from = from;
        this.to = to;
        this.kilometre = kilometre;
    }

    public Distance(Long idDistance, Integer from, Integer to, BigDecimal kilometre) {
        this.idDistance = idDistance;
        this.from = from;
        this.to = to;
        this.kilometre = kilometre;
    }

    public Long getIdDistance() {
        return idDistance;
    }

    public void setIdDistance(Long idDistance) {
        this.idDistance = idDistance;
    }

    public Integer getFrom() {
        return from;
    }

    public void setFrom(Integer from) {
        this.from = from;
    }

    public Integer getTo() {
        return to;
    }

    public void setTo(Integer to) {
        this.to = to;
    }

    public BigDecimal getKilometre() {
        return kilometre;
    }

    public void setKilometre(BigDecimal kilometre) {
        this.kilometre = kilometre;
    }
}
