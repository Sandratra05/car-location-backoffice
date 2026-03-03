<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Planification - Choisir une date - Car Location</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
</head>
<body>
<div class="container">
    <div class="header">
        <h1>📆 Planification des trajets</h1>
        <p>Choisissez une date pour générer le planning des véhicules</p>
    </div>

    <div class="content">
        <% if (request.getAttribute("error") != null) { %>
            <div class="alert alert-error"><%= request.getAttribute("error") %></div>
        <% } %>

        <form action="${pageContext.request.contextPath}/planning/planify" method="post">
            <div class="form-group">
                <label for="date">Date et heure (jour souhaité)</label>
                <input type="datetime-local" id="date" name="date" class="form-control" required>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Générer le planning</button>
                <a href="${pageContext.request.contextPath}/" class="btn btn-secondary">Accueil</a>
            </div>
        </form>
    </div>
</div>
</body>
</html>