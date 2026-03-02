package com.example.entity;

import java.util.List;

public class PlanningResult {
    private List<Trajet> trajets;
    private List<Reservation> nonAssignees;

    public PlanningResult() {}

    public PlanningResult(List<Trajet> trajets, List<Reservation> nonAssignees) {
        this.trajets = trajets;
        this.nonAssignees = nonAssignees;
    }

    public List<Trajet> getTrajets() {
        return trajets;
    }

    public void setTrajets(List<Trajet> trajets) {
        this.trajets = trajets;
    }

    public List<Reservation> getNonAssignees() {
        return nonAssignees;
    }

    public void setNonAssignees(List<Reservation> nonAssignees) {
        this.nonAssignees = nonAssignees;
    }
}
