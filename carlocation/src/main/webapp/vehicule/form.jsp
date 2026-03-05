<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.example.entity.Vehicule" %>
<%@ page import="com.example.enums.TypeCarburant" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <title>${title != null ? title : "Formulaire Véhicule"}</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/style.css">
    <style>
        .form-card {
            max-width: 520px;
            margin: 30px auto;
            background: white;
            border-radius: 10px;
            padding: 32px;
            box-shadow: 0 4px 16px rgba(0,0,0,0.10);
        }
        .form-group { margin-bottom: 20px; }
        .form-group label {
            display: block;
            font-weight: bold;
            margin-bottom: 6px;
            color: #1e3a5f;
        }
        .form-group input,
        .form-group select {
            width: 100%;
            padding: 10px 14px;
            border: 1px solid #ccc;
            border-radius: 6px;
            font-size: 1em;
            transition: border-color 0.2s;
        }
        .form-group input:focus,
        .form-group select:focus {
            border-color: #1e3a5f;
            outline: none;
            box-shadow: 0 0 0 2px rgba(30,58,95,0.15);
        }
        .btn-submit {
            width: 100%;
            padding: 12px;
            background: #1e3a5f;
            color: white;
            border: none;
            border-radius: 6px;
            font-size: 1.05em;
            font-weight: bold;
            cursor: pointer;
            transition: background 0.2s;
        }
        .btn-submit:hover { background: #16304f; }
        .btn-cancel {
            display: block;
            text-align: center;
            margin-top: 12px;
            color: #666;
            text-decoration: none;
            font-size: 0.95em;
        }
        .btn-cancel:hover { color: #1e3a5f; text-decoration: underline; }
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
   

    <div class="content">
        <div class="form-card">

            <%
                Vehicule vehicule = (Vehicule) request.getAttribute("vehicule");
                boolean isEdit = (vehicule != null && vehicule.getId() != null);
                TypeCarburant[] typeCarburants = (TypeCarburant[]) request.getAttribute("typeCarburants");
                if (typeCarburants == null) typeCarburants = TypeCarburant.values();
            %>

            <!-- TITRE -->
            <h2 style="color:#1e3a5f; margin-bottom:24px; text-align:center;">
                <%= isEdit ? "✏️ Modifier le Véhicule" : "➕ Nouveau Véhicule" %>
            </h2>

            <!-- MESSAGE ERREUR -->
            <% if (request.getAttribute("error") != null) { %>
            <div style="background:#f2dede; color:#a94442; padding:10px 14px; border-radius:6px; margin-bottom:20px; border:1px solid #ebccd1;">
                ❌ <%= request.getAttribute("error") %>
            </div>
            <% } %>

            <!-- FORMULAIRE -->
            <form method="post"
                  action="${pageContext.request.contextPath}/vehicules/<%= isEdit ? "update" : "create" %>">

                <!-- ID caché pour la modification -->
                <% if (isEdit) { %>
                <input type="hidden" name="id" value="<%= vehicule.getId() %>">
                <% } %>

                <!-- Référence -->
                <div class="form-group">
                    <label for="reference">Référence *</label>
                    <input type="text"
                           id="reference"
                           name="reference"
                           placeholder="Ex: VH-001"
                           value="<%= isEdit ? vehicule.getReference() : (request.getAttribute("reference") != null ? request.getAttribute("reference") : "") %>"
                           required>
                </div>

                <!-- Nombre de places -->
                <div class="form-group">
                    <label for="nbPlace">Nombre de places *</label>
                    <input type="number"
                           id="nbPlace"
                           name="nbPlace"
                           placeholder="Ex: 8"
                           min="1"
                           max="100"
                           value="<%= isEdit ? vehicule.getNbPlace() : (request.getAttribute("nbPlace") != null ? request.getAttribute("nbPlace") : "") %>"
                           required>
                </div>

                <!-- Type de carburant -->
                <div class="form-group">
                    <label for="typeCarburant">Type de carburant *</label>
                    <select id="typeCarburant" name="typeCarburant" required>
                        <option value="">-- Choisir --</option>
                        <% for (TypeCarburant tc : typeCarburants) {
                            boolean selected = isEdit && vehicule.getTypeCarburant() == tc;
                        %>
                        <option value="<%= tc.name() %>" <%= selected ? "selected" : "" %>>
                            <%= tc.name() %>
                        </option>
                        <% } %>
                    </select>
                </div>

                <!-- Bouton -->
                <button type="submit" class="btn-submit">
                    <%= isEdit ? "💾 Enregistrer les modifications" : "✅ Créer le véhicule" %>
                </button>

                <a href="${pageContext.request.contextPath}/vehicules" class="btn-cancel">
                    ← Retour à la liste
                </a>

            </form>
        </div><!-- /form-card -->
    </div><!-- /content -->
</div><!-- /container -->
</body>
</html>
