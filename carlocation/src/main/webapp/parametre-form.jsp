<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Parametre" %>
<%@ page import="java.util.Optional" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><%= request.getAttribute("title") != null ? request.getAttribute("title") : "Formulaire Paramètre" %> - Car Location</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="header">
        <h1>🚗 Car Location</h1>
        <p>Configuration des paramètres</p>
    </div>

    <div class="content">
        <h2 class="page-title">
            <%= request.getAttribute("title") != null ? request.getAttribute("title") : "Formulaire Paramètre" %>
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
            Parametre parametre = (Parametre) request.getAttribute("parametre");
            String formAction = request.getContextPath() + "/parametres/create";
        %>

        <div class="form-container">
            <form action="<%= formAction %>" method="post">

                <!-- Vitesse moyenne (km/h) -->
                <div class="form-group">
                    <label for="vitesseMoyenneKmh">Vitesse moyenne (km/h) *</label>
                    <input type="number"
                           step="0.01"
                           id="vitesseMoyenneKmh"
                           name="vitesseMoyenneKmh"
                           class="form-control"
                           required
                           value="<%= parametre != null && parametre.getVitesseMoyenneKmh() != null ? parametre.getVitesseMoyenneKmh().toString() : "" %>">
                </div>

                <!-- Temps d'attente (min) -->
                <div class="form-group">
                    <label for="tempsAttenteMin">Temps d'attente (min) *</label>
                    <input type="number"
                           id="tempsAttenteMin"
                           name="tempsAttenteMin"
                           class="form-control"
                           min="0"
                           required
                           value="<%= parametre != null && parametre.getTempsAttenteMin() != null ? parametre.getTempsAttenteMin() : "" %>">
                </div>

                <div class="form-actions">
                    <button type="submit" class="btn btn-success">💾 Enregistrer</button>
                    <a href="${pageContext.request.contextPath}/" class="btn btn-secondary">❌ Annuler</a>
                </div>

            </form>
        </div>
    </div>
</div>
</body>
</html>
