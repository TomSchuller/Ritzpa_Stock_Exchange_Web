<%--
    Document   : index
    Created on : Jan 24, 2012, 6:01:31 AM
    Author     : blecherl
    This is the login JSP for the online chat application
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <%@page import="utils.*" %>
    <%@ page import="constants.Constants" %>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Ritzpa Stock Exchange Market</title>
        <!--        Link the Bootstrap (from twitter) CSS framework in order to use its classes-->
        <link rel="stylesheet" href="common/bootstrap.min.css"/>
<!--        Link jQuery JavaScript library in order to use the $ (jQuery) method-->
<!--        <script src="script/jquery-2.0.3.min.js"></script>-->
<!--        and\or any other scripts you might need to operate the JSP file behind the scene once it arrives to the client-->
        <style>
            body, html {
                height: 100%;
            }

            body {
                background-image: linear-gradient(rgba(255,255,255,0.5), rgba(255,255,255,0.5)), url("common/images/wolf_of_wall_street.jpg");
                height: 100%;
                background-position: center;
                background-repeat: no-repeat;
                background-size: cover;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <% String usernameFromSession = SessionUtils.getUsername(request);%>
            <% String usernameFromParameter = request.getParameter(Constants.USERNAME) != null ? request.getParameter(Constants.USERNAME) : "";%>
            <% if (usernameFromSession == null) {%>
            <h1>Welcome to Ritzpa Stock Exchange Market!</h1>
            <br/>
            <h2>Please enter a user name:</h2>
            <form method="GET" action="feedPage">
                <input type="text" name="username" class="" placeholder="Enter User Name" />
                <input type="submit" value="Login"/>
                <br><br>
                <div>
                    <input type="radio" id="adminChoice"
                           name="choice" value="admin">
                    <label for="adminChoice">Admin</label>
                    <input type="radio" id="brokerChoice"
                           name="choice" value="broker">
                    <label for="brokerChoice">Broker</label>
                </div>
            </form>
            <% Object errorMessage = request.getAttribute(Constants.USER_NAME_ERROR);%>
            <% if (errorMessage != null) {%>
            <span class="bg-danger" style="color:red;"><%=errorMessage%></span>
            <% } %>
            <% } else {%>
            <h1>Welcome back, <%=usernameFromSession%></h1>
            <a href="../chatroom/brokerFeedPage.html">Click here to enter the chat room</a>
            <br/>
            <a href="login?logout=true" id="logout">logout</a>
            <% }%>
        </div>
    </body>
</html>