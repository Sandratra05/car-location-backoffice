<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Vehicule" %>
<%@ page import="com.example.entity.Reservation" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Comparator" %>
<%@ page import="java.math.BigDecimal" %>
<%@ page import="java.sql.Timestamp" %>

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
        <div class="card">
            <h3>Réservations assignées</h3>

            <%
                SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss");
                Map assignments = (Map) request.getAttribute("assignments");
                if (assignments == null || assignments.isEmpty()) {
            %>
                <div class="alert">Aucune assignation trouvée pour cette date.</div>
            <% } else { %>

                <table class="table table-striped" style="width:100%; border-collapse:collapse; margin-bottom:20px; margin-top:8px;">
                    <thead style="background:#1e3a5f; text-align:left;">
                        <tr>
                            <th style="padding:8px; border:1px solid #ddd;">Véhicule</th>
                            <th style="padding:8px; border:1px solid #ddd;">Réservations</th>
                            <th style="padding:8px; border:1px solid #ddd;">Trajet</th>
                            <th style="padding:8px; border:1px solid #ddd;">Km parcouru</th>
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

                            // Récupérer les données préparées
                            String trajet = (String) ((Map) request.getAttribute("routes")).get(v);
                            Timestamp vehicleDepart = (Timestamp) ((Map) request.getAttribute("departTimes")).get(v);
                            Timestamp vehicleReturn = (Timestamp) ((Map) request.getAttribute("returnTimes")).get(v);
                            BigDecimal km = (BigDecimal) ((Map) request.getAttribute("kmMap")).get(v);

                            // Construire détails réservations
                            String details = "";
                            for (Reservation r : resList) {
                                if (!details.isEmpty()) details += "<br/>";
                                details += "Resa num " + r.getIdReservation() + " (" + r.getNbPassager() + " prs - " + (r.getHotel() != null ? r.getHotel().getLibelle() : "-") + ")";
                            }
                    %>
                        <tr>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                                <strong><%= v.getReference() %></strong><br/>
                                <small><%= v.getNbPlace() %> places • <%= v.getTypeCarburant() %></small>
                            </td>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top;"><%= details %></td>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top;"><%= trajet %></td>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top; text-align:right;">
                                <%= km != null ? km.setScale(2, java.math.RoundingMode.HALF_UP) + " km" : "-" %>
                            </td>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                                <%= vehicleDepart != null ? timeFmt.format(vehicleDepart) : "-" %>
                            </td>
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top;">
                                <%= vehicleReturn != null ? timeFmt.format(vehicleReturn) : "-" %>
                            </td>
                        </tr>
                    <%      }
                    %>
                    </tbody>
                </table>
        </div>
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
                                </tr>
                            </thead>
                            <tbody>
                            <% for (Reservation r : unassigned) { %>
                                <tr>
                                    <td style="padding:8px; border:1px solid #ddd;">#<%= r.getIdReservation() %></td>
                                    <td style="padding:8px; border:1px solid #ddd; text-align:right;"><%= r.getNbPassager() %></td>
                                    <td style="padding:8px; border:1px solid #ddd;"><%= r.getHotel() != null ? r.getHotel().getLibelle() : "-" %></td>
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