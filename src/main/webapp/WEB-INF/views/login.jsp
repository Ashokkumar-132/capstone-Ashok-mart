<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Log in | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="auth-layout">
    <section class="auth-intro" aria-labelledby="login-heading">
        <p class="eyebrow">Welcome back</p>
        <h1>Your marketplace, your choice.</h1>
        <p class="lede">Sign in to keep your AshokMart experience in one place.</p>
    </section>
    <section class="auth-card" aria-labelledby="login-heading">
        <h2 id="login-heading">Log in</h2>
        <p class="form-intro">Use the email and password associated with your account.</p>
        <c:if test="${not empty requestScope.loginError}">
            <div class="form-message error" role="alert">${requestScope.loginError}</div>
        </c:if>
        <c:if test="${param.registered == 'true'}">
            <div class="form-message success" role="status">Your account is ready. You can log in now.</div>
        </c:if>
        <form action="${pageContext.request.contextPath}/login" method="post">
            <div class="form-field">
                <label for="email">Email address</label>
                <input id="email" name="email" type="email" autocomplete="email" required maxlength="255" value="${param.email}">
            </div>
            <div class="form-field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="current-password" required>
            </div>
            <button class="primary-button" type="submit">Log in</button>
        </form>
        <p class="form-footer">New to AshokMart? <a href="${pageContext.request.contextPath}/register">Create an account</a></p>
    </section>
</main>
</body>
</html>
