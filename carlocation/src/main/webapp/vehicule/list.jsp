<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.List" %>
<%@ page import="com.example.entity.Vehicule" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>${title != null ? title : "Liste des Véhicules"}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .btn-sm { padding: 5px 12px; font-size: 0.85em; }
        .btn-edit { background: #f0ad4e; color: white; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; }
        .btn-edit:hover { background: #ec971f; }
        .btn-delete { background: #d9534f; color: white; border: none; border-radius: 4px; cursor: pointer; text-decoration: none; }
        .btn-delete:hover { background: #c9302c; }
        .badge-diesel    { background: #1e3a5f; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.8em; }
        .badge-essence   { background: #5bc0de; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.8em; }
        .badge-hybride   { background: #5cb85c; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.8em; }
        .badge-electrique{ background: #9b59b6; color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.8em; }
        tr.row-even { background: #ffffff; }
        tr.row-odd  { background: #f8f9fa; }
    </style>
</head>
<body>
<div class="container">

    <!-- HEADER -->
    <div class="header">
        <h1>🚗 Gestion des Véhicules</h1>
        <p>Car Location Backoffice</p>
    </div>

    <!-- NAVIGATION -->
    

    <div class="content" style="padding: 30px;">

        <!-- MESSAGES -->
        <% if (request.getAttribute("success") != null) { %>
        <div class="alert" style="background:#dff0d8; color:#3c763d; padding:12px 16px; border-radius:6px; margin-bottom:20px; border:1px solid #d6e9c6;">
            ✅ <%= request.getAttribute("success") %>
        </div>
        <% } %>
        <% if (request.getAttribute("error") != null) { %>
        <div class="alert" style="background:#f2dede; color:#a94442; padding:12px 16px; border-radius:6px; margin-bottom:20px; border:1px solid #ebccd1;">
            ❌ <%= request.getAttribute("error") %>
        </div>
        <% } %>

        <!-- TITRE + BOUTON AJOUTER -->
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:20px;">
            <h2 style="color:#1e3a5f;">Liste des Véhicules</h2>
            <a href="${pageContext.request.contextPath}/vehicules/new"
               style="background:#1e3a5f; color:white; padding:10px 20px; border-radius:6px; text-decoration:none; font-weight:bold;">
                ➕ Nouveau Véhicule
            </a>
        </div>

        <!-- TABLEAU -->
        <%
            List<Vehicule> vehicules = (List<Vehicule>) request.getAttribute("vehicules");
            if (vehicules == null || vehicules.isEmpty()) {
        %>
        <div style="text-align:center; padding:40px; color:#999; background:#f9f9f9; border-radius:8px;">
            <p style="font-size:1.2em;">Aucun véhicule trouvé.</p>
            <a href="${pageContext.request.contextPath}/vehicules/new"
               style="display:inline-block; margin-top:15px; background:#1e3a5f; color:white; padding:10px 20px; border-radius:6px; text-decoration:none;">
                ➕ Ajouter le premier véhicule
            </a>
        </div>
        <% } else { %>
        <table style="width:100%; border-collapse:collapse; background:white; border-radius:8px; overflow:hidden; box-shadow:0 2px 8px rgba(0,0,0,0.08);">
            <thead>
            <tr style="background:#1e3a5f; color:white;">
                <th style="padding:14px 16px; text-align:left;">#</th>
                <th style="padding:14px 16px; text-align:left;">Référence</th>
                <th style="padding:14px 16px; text-align:center;">Nb Places</th>
                <th style="padding:14px 16px; text-align:center;">Carburant</th>
                <th style="padding:14px 16px; text-align:center;">Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                int i = 0;
                for (Vehicule v : vehicules) {
                    String rowClass = (i % 2 == 0) ? "row-even" : "row-odd";
                    String carburant = v.getTypeCarburant() != null ? v.getTypeCarburant().toString() : "-";
                    String badgeClass = "badge-" + carburant.toLowerCase();
                    i++;
            %>
            <tr class="<%= rowClass %>" style="border-bottom:1px solid #e9ecef;">
                <td style="padding:12px 16px; color:#999;"><%= v.getId() %></td>
                <td style="padding:12px 16px; font-weight:bold;"><%= v.getReference() %></td>
                <td style="padding:12px 16px; text-align:center;"><%= v.getNbPlace() %> places</td>
                <td style="padding:12px 16px; text-align:center;">
                    <span class="<%= badgeClass %>"><%= carburant %></span>
                </td>
                <td style="padding:12px 16px; text-align:center;">
                    <a href="${pageContext.request.contextPath}/vehicules/edit?id=<%= v.getId() %>"
                       class="btn-sm btn-edit" style="margin-right:6px;">✏️ Modifier</a>
                    <a href="${pageContext.request.contextPath}/vehicules/delete?id=<%= v.getId() %>"
                       class="btn-sm btn-delete"
                       onclick="return confirm('Supprimer le véhicule <%= v.getReference() %> ?');">
                        🗑️ Supprimer
                    </a>
                </td>
            </tr>
            <% } %>
            </tbody>
        </table>
        <p style="margin-top:12px; color:#999; font-size:0.9em;"><%= vehicules.size() %> véhicule(s) au total.</p>
        <% } %>

    </div><!-- /content -->
</div><!-- /container -->
</body>
</html>
