<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AshokMart | Your marketplace, your choice</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="home-layout">
    <section class="home-card" aria-labelledby="home-heading">
        <p class="eyebrow">Foundation ready</p>
        <h1 id="home-heading">Your marketplace, your choice.</h1>
        <p class="lede">AshokMart is getting ready for a better way to browse, buy, and sell. Authentication is now available while the marketplace modules are built.</p>
        <div class="home-actions">
            <c:choose>
                <c:when test="${empty sessionScope.authenticatedUser}">
                    <a href="${pageContext.request.contextPath}/login">Log in</a>
                    <a href="${pageContext.request.contextPath}/register">Create an account</a>
                </c:when>
                <c:otherwise>
                    <span class="account-label">Signed in as ${sessionScope.authenticatedUser.name}</span>
                </c:otherwise>
            </c:choose>
        </div>
    </section>
</main>
</body>
</html>
