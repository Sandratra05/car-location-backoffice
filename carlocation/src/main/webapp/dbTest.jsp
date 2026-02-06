<%@ page contentType="text/html; charset=UTF-8" language="java" %>
<%@ page import="java.util.List"%>
<html>
<head>
    <title>DB Test</title>
</head>
<body>
<h2>Messages depuis la base de données</h2>
<%
    List<String> dbMessages = (List) request.getAttribute("dbMessages");
    if (dbMessages != null && !dbMessages.isEmpty()) {
%>
    <ul>
    <%
        for (String m : dbMessages) {
    %>
        <li><%= m %></li>
    <%
        }
    %>
    </ul>
<%
    } else {
        String error = (String) request.getAttribute("dbMessage");
%>
    <p><%= (error != null) ? error : "Aucun message trouvé." %></p>
<%
    }
%>
</body>
</html>