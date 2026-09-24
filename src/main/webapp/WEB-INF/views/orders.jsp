<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My Orders | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/orders.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout orders-layout">
    <div class="orders-heading">
        <div><p class="eyebrow">Your AshokMart history</p><h1>My Orders</h1></div>
        <a class="back-link" href="${pageContext.request.contextPath}/products">← Continue shopping</a>
    </div>

    <c:choose>
        <c:when test="${empty orders}">
            <section class="empty-state" aria-labelledby="empty-orders-heading">
                <span class="empty-mark" aria-hidden="true">—</span>
                <h2 id="empty-orders-heading">You haven't placed any orders yet.</h2>
                <p>Find something you love and your completed purchases will appear here.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/products">Start shopping</a>
            </section>
        </c:when>
        <c:otherwise>
            <section class="order-list" aria-labelledby="order-list-heading">
                <h2 id="order-list-heading" class="sr-only">Order history</h2>
                <c:forEach items="${orders}" var="order">
                    <article class="order-card">
                        <div class="order-card-main">
                            <div><p class="order-label">Order #${order.id}</p><h2>${order.createdAt}</h2></div>
                            <span class="order-status status-${fn:toLowerCase(order.status)}"><span class="status-dot" aria-hidden="true"></span>${order.status}</span>
                        </div>
                        <div class="order-card-meta"><span>${order.itemCount} item<c:if test="${order.itemCount != 1}">s</c:if></span><strong><fmt:formatNumber value="${order.totalAmount}" type="currency" currencyCode="USD" /></strong></div>
                        <a class="secondary-button order-details-link" href="${pageContext.request.contextPath}/orders/view?id=${order.id}">View details</a>
                    </article>
                </c:forEach>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
