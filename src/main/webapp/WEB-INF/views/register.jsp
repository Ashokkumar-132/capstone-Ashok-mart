<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create account | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="auth-layout">
    <section class="auth-intro" aria-labelledby="register-heading">
        <p class="eyebrow">Join AshokMart</p>
        <h1>Make room for better finds.</h1>
        <p class="lede">Create your buyer account and discover a marketplace built around choice.</p>
    </section>
    <section class="auth-card" aria-labelledby="register-heading">
        <h2 id="register-heading">Create an account</h2>
        <p class="form-intro">Public accounts start as buyer accounts. You can update your profile later.</p>
        <c:if test="${not empty requestScope.registerError}">
            <div class="form-message error" role="alert">${requestScope.registerError}</div>
        </c:if>
        <form action="${pageContext.request.contextPath}/register" method="post" data-register-form>
            <div class="form-field">
                <label for="name">Full name</label>
                <input id="name" name="name" type="text" autocomplete="name" required maxlength="120" value="${param.name}">
            </div>
            <div class="form-field">
                <label for="email">Email address</label>
                <input id="email" name="email" type="email" autocomplete="email" required maxlength="255" value="${param.email}">
            </div>
            <div class="form-field">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" autocomplete="new-password" required minlength="8">
                <span class="password-hint">Use at least 8 characters.</span>
            </div>
            <div class="form-field">
                <label for="confirmPassword">Confirm password</label>
                <input id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" required minlength="8">
            </div>
            <button class="primary-button" type="submit">Create account</button>
        </form>
        <p class="form-footer">Already have an account? <a href="${pageContext.request.contextPath}/login">Log in</a></p>
    </section>
</main>
<script src="${pageContext.request.contextPath}/js/auth.js" defer></script>
</body>
</html>
