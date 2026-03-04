<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Vehicule" %>
<%@ page import="com.example.entity.Reservation" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Planning - Résultats - Car Location</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="header">
        <h1>🗺️ Planning des trajets</h1>
    </div>

    <div class="content">
        <a href="${pageContext.request.contextPath}/planning/new" class="btn btn-secondary">← Changer la date</a>
        <hr/>

        <%
            SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
            Map assignments = (Map) request.getAttribute("assignments");
            if (assignments == null || assignments.isEmpty()) {
        %>
            <div class="alert">Aucune assignation trouvée pour cette date.</div>
        <% } else { %>

            <table class="table table-striped" style="width:100%; border-collapse:collapse; margin-bottom:20px;">
                <thead style="background:#1e3a5f; text-align:left;">
                    <tr>
                        <th style="padding:8px; border:1px solid #ddd;">Véhicule</th>
                        <th style="padding:8px; border:1px solid #ddd;">Num Réservation</th>
                        <th style="padding:8px; border:1px solid #ddd;">Nb Passagers</th>
                        <th style="padding:8px; border:1px solid #ddd;">Hôtel</th>
                        <th style="padding:8px; border:1px solid #ddd;">Départ véhicule</th>
                        <th style="padding:8px; border:1px solid #ddd;">Retour aéroport</th>
                    </tr>
                </thead>
                <tbody>
                <%
                    for (Object key : assignments.keySet()) {
                        Vehicule v = (Vehicule) key;
                        List<Reservation> resList = (List<Reservation>) assignments.get(key);
                        if (resList == null || resList.isEmpty()) continue;

                        // Calculer l'heure de retour du véhicule pour toutes ses réservations
                        java.sql.Timestamp vehicleReturn = null;
                        try {
                            if (!resList.isEmpty()) {
                                vehicleReturn = resList.get(0).calculHeureRetourTotal(resList);
                            }
                        } catch (Exception e) {
                            // Ignorer les erreurs
                        }

                        for (Reservation r : resList) {
                %>
                    <tr>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                            <strong><%= v.getReference() %></strong><br/>
                            <small><%= v.getNbPlace() %> places • <%= v.getTypeCarburant() %></small>
                        </td>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">#<%= r.getIdReservation() %></td>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top; text-align:right;"><%= r.getNbPassager() %></td>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top;"><%= r.getHotel() != null ? r.getHotel().getLibelle() : "-" %></td>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                            <%= r.calculHeureDeDepart() != null ? timeFmt.format(r.calculHeureDeDepart()) : "-" %>
                        </td>
                        <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                            <%= vehicleReturn != null ? timeFmt.format(vehicleReturn) : "-" %>
                        </td>
                    </tr>
                <%      }
                    }
                %>
                </tbody>
            </table>

        <% } %>
        <%    
            List<Reservation> unassigned = (List<Reservation>) request.getAttribute("unassigned");
            if (unassigned != null && !unassigned.isEmpty()) {
        %>
                <hr/>
                <div class="card">
                        <h3>Réservations non assignées</h3>
                        <table style="width:100%; border-collapse:collapse; margin-top:8px;">
                            <thead style="background:#1e3a5f; text-align:left;">
                                <tr>
                                    <th style="padding:8px; border:1px solid #ddd;">Num Réservation</th>
                                    <th style="padding:8px; border:1px solid #ddd;">Nb Passagers</th>
                                    <th style="padding:8px; border:1px solid #ddd;">Hôtel</th>
                                    <th style="padding:8px; border:1px solid #ddd;">Départ véhicule</th>
                                    <th style="padding:8px; border:1px solid #ddd;">Retour aéroport</th>
                                </tr>
                            </thead>
                            <tbody>
                            <% for (Reservation r : unassigned) { %>
                                <tr>
                                    <td style="padding:8px; border:1px solid #ddd;">#<%= r.getIdReservation() %></td>
                                    <td style="padding:8px; border:1px solid #ddd; text-align:right;"><%= r.getNbPassager() %></td>
                                    <td style="padding:8px; border:1px solid #ddd;"><%= r.getHotel() != null ? r.getHotel().getLibelle() : "-" %></td>
                                    <td style="padding:8px; border:1px solid #ddd; text-align:center;"><%= r.calculHeureDeDepart() != null ? timeFmt.format(r.calculHeureDeDepart()) : "-" %></td>
                                    <td style="padding:8px; border:1px solid #ddd; text-align:center;"><%= r.calculHeureRetour() != null ? timeFmt.format(r.calculHeureRetour()) : "-" %></td>
                                </tr>
                            <% } %>
                            </tbody>
                        </table>
                </div>
        <%  } %>
    </div>
</div>
</body>
</html>