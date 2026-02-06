<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Reservation" %>
<%@ page import="com.example.entity.Hotel" %>
<%@ page import="java.util.List" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= request.getAttribute("title") != null ? request.getAttribute("title") : "Formulaire Réservation" %> - Car Location</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="header">
        <h1>🚗 Car Location</h1>
        <p>Gestion de location de véhicules</p>
    </div>

    <div class="content">
        <h2 class="page-title">
            <%= request.getAttribute("title") != null ? request.getAttribute("title") : "Formulaire Réservation" %>
        </h2>

            <% if (request.getAttribute("error") != null) { %>
                <div class="alert alert-error">
                    <%= request.getAttribute("error") %>
                </div>
            <% } %>

            <% if (request.getAttribute("success") != null) { %>
                <div class="alert alert-success">
                    <%= request.getAttribute("success") %>
                </div>
            <% } %>


        <%
            Reservation reservation = (Reservation) request.getAttribute("reservation");
            String formAction = request.getContextPath() + "/reservations/create";
            List<Hotel> hotels = (List<Hotel>) request.getAttribute("hotels");
        %>

        <div class="form-container">
            <form action="<%= formAction %>" method="post">

                <!-- Nombre de passagers -->
                <div class="form-group">
                    <label for="nbPassager">Nombre de passagers *</label>
                    <input type="number"
                           id="nbPassager"
                           name="nbPassager"
                           class="form-control"
                           min="1"
                           required
                           value="<%= reservation != null ? reservation.getNbPassager() : "" %>">
                </div>

                <!-- Date heure arrivée -->
                <div class="form-group">
                    <label for="dateHeureArrivee">Date et heure d'arrivée *</label>
                    <input type="datetime-local"
                           id="dateHeureArrivee"
                           name="dateHeureArrivee"
                           class="form-control"
                           required
                           value="<%= reservation != null && reservation.getDateHeureArrivee() != null
                               ? reservation.getDateHeureArrivee().toString().substring(0,16)
                               : "" %>">
                </div>

                <!-- Hôtel -->
                <div class="form-group">
                    <label for="idHotel">Hôtel *</label>
                    <select id="idHotel" name="idHotel" class="form-control" required>
                        <option value="">-- Sélectionner --</option>
                        <% if (hotels != null) {
                            for (Hotel hotel : hotels) { %>
                                <option value="<%= hotel.getIdHotel() %>"
                                    <%= reservation != null &&
                                        reservation.getHotel() != null &&
                                        reservation.getHotel().getIdHotel().equals(hotel.getIdHotel())
                                        ? "selected" : "" %>>
                                    <%= hotel.getLibelle() %> (<%= hotel.getDistance() %> km)
                                </option>
                        <%  }
                        } %>
                    </select>
                </div>

                <!-- Client (ID simple) -->
                <div class="form-group">
                    <label for="idClient">ID Client *</label>
                    <input type="text"
                           id="idClient"
                           name="idClient"
                           class="form-control"
                           maxlength="4"
                           required
                           value="<%= reservation != null ? reservation.getIdClient() : "" %>">
                </div>

                <div class="form-actions">
                    <button type="submit" class="btn btn-success">💾 Enregistrer</button>
                    <a href="${pageContext.request.contextPath}/reservations" class="btn btn-secondary">❌ Annuler</a>
                </div>

            </form>
        </div>
    </div>
</div>
</body>
</html>
