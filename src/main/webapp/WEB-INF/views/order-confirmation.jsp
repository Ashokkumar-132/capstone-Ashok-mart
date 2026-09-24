<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:choose><c:when test="${orderNotFound}">Order not found</c:when><c:otherwise>Order confirmed | AshokMart</c:otherwise></c:choose></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/checkout.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout confirmation-layout">
    <c:choose>
        <c:when test="${orderNotFound}">
            <section class="empty-state" aria-labelledby="order-not-found-heading">
                <p class="eyebrow">Order unavailable</p>
                <h1 id="order-not-found-heading">We could not find that order.</h1>
                <p>It may not belong to this account or may no longer be available.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/products">Continue shopping</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="confirmation-hero" aria-labelledby="confirmation-heading">
                <span class="confirmation-check" aria-hidden="true">✓</span>
                <p class="eyebrow">Thank you for shopping with AshokMart</p>
                <h1 id="confirmation-heading">Order placed successfully.</h1>
                <p>Your order is confirmed and ready for the next step.</p>
                <div class="order-meta"><span>Order #${order.id}</span><span>Status: ${order.status}</span><span>${order.createdAt}</span></div>
            </section>
            <section class="confirmation-card" aria-labelledby="purchased-items-heading">
                <div class="confirmation-card-heading"><h2 id="purchased-items-heading">Purchased items</h2><strong><fmt:formatNumber value="${order.totalAmount}" type="currency" currencyCode="USD" /></strong></div>
                <c:forEach items="${orderItems}" var="item">
                    <div class="confirmation-item"><span>${item.productName} <small>× ${item.quantity}</small></span><strong><fmt:formatNumber value="${item.subtotal}" type="currency" currencyCode="USD" /></strong></div>
                </c:forEach>
            </section>
            <div class="confirmation-actions">
                <a class="primary-button" href="${pageContext.request.contextPath}/products">Continue shopping</a>
                <a class="secondary-button" href="${pageContext.request.contextPath}/cart">View cart</a>
            </div>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
