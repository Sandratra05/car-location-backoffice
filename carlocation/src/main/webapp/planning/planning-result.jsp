<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Vehicule" %>
<%@ page import="com.example.entity.Reservation" %>
<%@ page import="java.util.*" %>

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
            Map assignments = (Map) request.getAttribute("assignments");
            if (assignments == null || assignments.isEmpty()) {
        %>
            <div class="alert">Aucune assignation trouvée pour cette date.</div>
        <% } else {
                for (Object key : assignments.keySet()) {
                    Vehicule v = (Vehicule) key;
                    List<Reservation> resList = (List<Reservation>) assignments.get(key);
        %>
                    <div class="card">
                        <h3><%= v.getReference() %> — <%= v.getNbPlace() %> places — <%= v.getTypeCarburant() %></h3>
                        <ul>
                            <% for (Reservation r : resList) { %>
                                <li>
                                    Réservation #<%= r.getIdReservation() %> — <%= r.getNbPassager() %> passagers — Hôtel: <%= r.getHotel() != null ? r.getHotel().getLibelle() : "-" %>
                                    <br/>
                                    Départ véhicule: <%= r.calculHeureDeDepart() != null ? r.calculHeureDeDepart().toString() : "-" %>
                                    <br/>
                                    Retour à l'aéroport: <%= r.calculHeureRetour() != null ? r.calculHeureRetour().toString() : "-" %>
                                </li>
                            <% } %>
                        </ul>
                    </div>
        <%      }
            }
        %>
    </div>
</div>
</body>
</html>