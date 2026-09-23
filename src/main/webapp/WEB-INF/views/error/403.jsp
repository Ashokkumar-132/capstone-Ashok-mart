<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Access unavailable | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="home-layout">
    <section class="home-card" aria-labelledby="forbidden-heading">
        <p class="eyebrow">403 · Access unavailable</p>
        <h1 id="forbidden-heading">You do not have permission to view this page.</h1>
        <p class="lede">The requested area is restricted for this account. You can return to the AshokMart home page or continue with an account that has the required access.</p>
        <div class="home-actions">
            <a href="${pageContext.request.contextPath}/">Return home</a>
        </div>
    </section>
</main>
</body>
</html>
