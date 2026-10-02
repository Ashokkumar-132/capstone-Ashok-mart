<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>AshokMart | Your marketplace, your choice</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="home-layout">
    <section class="home-card home-hero" aria-labelledby="home-heading">
        <div><p class="eyebrow">A marketplace with more choice</p><h1 id="home-heading">Find good things from sellers worth discovering.</h1><p class="lede">Browse a growing collection, buy with confidence, and keep every order in one simple place.</p><div class="home-actions"><a href="${pageContext.request.contextPath}/products">Explore products</a><c:choose><c:when test="${empty sessionScope.authenticatedUser}"><a href="${pageContext.request.contextPath}/register">Join AshokMart</a></c:when><c:otherwise><a href="${pageContext.request.contextPath}/orders">View your orders</a></c:otherwise></c:choose></div></div>
        <div class="home-hero-note"><span class="hero-kicker">Designed for</span><strong>Buyers, sellers, and the next great find.</strong><span>Secure sessions · clear order history · real seller ownership</span></div>
    </section>
    <section class="home-benefits" aria-labelledby="benefits-heading"><p class="eyebrow">Why AshokMart</p><h2 id="benefits-heading">A clearer way to shop and sell.</h2><div class="benefit-grid"><article><span class="benefit-number">01</span><h3>Browse with intent</h3><p>Search, filter, sort, and compare products without the clutter.</p></article><article><span class="benefit-number">02</span><h3>Buy with confidence</h3><p>Server-backed checkout, historical order details, and buyer reviews keep the experience dependable.</p></article><article><span class="benefit-number">03</span><h3>Sell with control</h3><p>Independent sellers manage their own catalog and fulfillment workflow.</p></article></div></section>
</main>
<jsp:include page="/WEB-INF/views/includes/footer.jsp" />
</body>
</html>
