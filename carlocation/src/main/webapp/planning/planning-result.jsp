<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Vehicule" %>
<%@ page import="com.example.entity.Reservation" %>
<%@ page import="java.util.*" %>
<%@ page import="java.util.Collections" %>
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
            <% } else {
                // Récupérer les heures de départ pour le tri
                final Map<Vehicule, Timestamp> departTimesMap = (Map<Vehicule, Timestamp>) request.getAttribute("departTimes");

                // Créer une liste triée par heure de départ
                List<Vehicule> sortedVehicles = new ArrayList<Vehicule>(assignments.keySet());
                Collections.sort(sortedVehicles, new Comparator<Vehicule>() {
                    public int compare(Vehicule v1, Vehicule v2) {
                        Timestamp t1 = departTimesMap != null ? departTimesMap.get(v1) : null;
                        Timestamp t2 = departTimesMap != null ? departTimesMap.get(v2) : null;
                        if (t1 == null && t2 == null) return 0;
                        if (t1 == null) return 1;
                        if (t2 == null) return -1;
                        int cmp = t1.compareTo(t2);
                        if (cmp != 0) return cmp;
                        // Si même heure, trier par référence véhicule
                        return v1.getReference().compareTo(v2.getReference());
                    }
                });
            %>

                <table class="table table-striped" style="width:100%; border-collapse:collapse; margin-bottom:20px; margin-top:8px;">
                    <thead style="background:#1e3a5f; text-align:left;">
                        <tr>
                            <th style="padding:8px; border:1px solid #ddd;">Véhicule</th>
                            <th style="padding:8px; border:1px solid #ddd;">Réservations</th>
                            <th style="padding:8px; border:1px solid #ddd;">Trajet</th>
                            <th style="padding:8px; border:1px solid #ddd;">Km parcouru</th>
                            
                            <th style="padding:8px; border:1px solid #ddd;">Places</th>
                            <th style="padding:8px; border:1px solid #ddd;">Départ véhicule</th>
                            <th style="padding:8px; border:1px solid #ddd;">Retour aéroport</th>
                        </tr>
                    </thead>
                    <tbody>
                    <%
                        String lastDepartTime = "";
                        for (Vehicule v : sortedVehicles) {
                            List<Reservation> resList = (List<Reservation>) assignments.get(v);
                            if (resList == null || resList.isEmpty()) continue;

                            // Récupérer les données préparées
                            String trajet = (String) ((Map) request.getAttribute("routes")).get(v);
                            Timestamp vehicleDepart = departTimesMap != null ? departTimesMap.get(v) : null;
                            Timestamp vehicleReturn = (Timestamp) ((Map) request.getAttribute("returnTimes")).get(v);
                            BigDecimal km = (BigDecimal) ((Map) request.getAttribute("kmMap")).get(v);
                            Integer nbTrajets = (Integer) ((Map) request.getAttribute("trajetsMap")).get(v);
                            String places = (String) ((Map) request.getAttribute("placesMap")).get(v);
                            Integer occupied = (Integer) ((Map) request.getAttribute("occupiedMap")).get(v);
                            Integer tempsAttenteMin = (Integer) request.getAttribute("tempsAttenteMin");

                            // Vérifier si c'est un nouveau groupe horaire
                            String currentDepartTime = vehicleDepart != null ? timeFmt.format(vehicleDepart) : "-";
                            boolean isNewTimeGroup = !currentDepartTime.equals(lastDepartTime);
                            lastDepartTime = currentDepartTime;

                            // Construire détails réservations
                            String details = "";
                            for (Reservation r : resList) {
                                if (!details.isEmpty()) details += "<br/>";
                                details += "- Clients num " + r.getIdReservation() + " <br/> ( <strong> " + r.getNbPassager() + " prs </strong> - " + (r.getHotel() != null ? r.getHotel().getLibelle() : "-") + ")";
                            }
                    %>
                        <% if (isNewTimeGroup) { %>
                        <tr style="background:#e8f4f8;">
                            <td colspan="7" style="padding:8px; border:1px solid #ddd; font-weight:bold; color:#1e3a5f;">
                                Départ à <%= currentDepartTime %>
                            </td>
                        </tr>
                        <% } %>
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
                            <td style="padding:8px; border:1px solid #ddd; vertical-align:top; text-align:center;">
                                <% int tot = v.getNbPlace(); int occ = 0; for (Reservation rr : resList) { if (rr.getNbPassager() != null) occ += rr.getNbPassager(); } int free = tot - occ; %>
                                <%= occ %>/<%= tot %> places<%= free > 0 ? " (" + free + " libres)" : " (complet)" %>
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
                                    <th style="padding:8px; border:1px solid #ddd;">Statut</th>
                                </tr>
                            </thead>
                            <tbody>
                            <% for (Reservation r : unassigned) { %>
                                <tr>
                                    <td style="padding:8px; border:1px solid #ddd;">#<%= r.getIdReservation() %> (reportée)</td>
                                    <td style="padding:8px; border:1px solid #ddd; text-align:right;"><%= r.getNbPassager() %></td>
                                    <td style="padding:8px; border:1px solid #ddd;"><%= r.getHotel() != null ? r.getHotel().getLibelle() : "-" %></td>
                                    <td style="padding:8px; border:1px solid #ddd; color:orange; font-weight:bold;">Reportée</td>
                                </tr>
                            <% } %>
                            </tbody>
                        </table>
                </div>
        <%  } %>

        <%
            Map trajetsSummary = (Map) request.getAttribute("trajetsSummary");
            if (trajetsSummary != null && !trajetsSummary.isEmpty()) {
        %>
            <hr/>
            <div class="card">
                <h3>Nombre de trajets par véhicule (aujourd'hui)</h3>
                <table style="width:100%; border-collapse:collapse; margin-top:8px;">
                    <thead style="background:#f4f4f4; text-align:left;">
                        <tr>
                            <th style="padding:6px; border:1px solid #ddd;">Véhicule</th>
                            <th style="padding:6px; border:1px solid #ddd;">Trajets</th>
                        </tr>
                    </thead>
                    <tbody>
                    <% for (Object k : trajetsSummary.keySet()) {
                           Object val = trajetsSummary.get(k);
                    %>
                        <tr>
                            <td style="padding:6px; border:1px solid #ddd;"><strong><%= k %></strong></td>
                            <td style="padding:6px; border:1px solid #ddd;"><%= val != null ? val : 0 %></td>
                        </tr>
                    <% } %>
                    </tbody>
                </table>
            </div>
        <% } %>
    </div>
</div>
</body>
</html>