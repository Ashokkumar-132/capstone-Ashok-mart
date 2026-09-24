<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:choose><c:when test="${orderNotFound}">Order not found</c:when><c:otherwise>Order #${orderDetails.order.id} | AshokMart</c:otherwise></c:choose></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/orders.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout order-details-layout">
    <c:choose>
        <c:when test="${orderNotFound}">
            <section class="empty-state" aria-labelledby="order-not-found-heading">
                <p class="eyebrow">Order unavailable</p>
                <h1 id="order-not-found-heading">We could not find that order.</h1>
                <p>It may not belong to this account or may no longer be available.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/orders">Back to my orders</a>
            </section>
        </c:when>
        <c:otherwise>
            <div class="order-details-heading"><div><p class="eyebrow">Purchase record</p><h1>Order #${orderDetails.order.id}</h1></div><a class="back-link" href="${pageContext.request.contextPath}/orders">← Back to orders</a></div>
            <section class="order-summary-card" aria-labelledby="order-summary-heading">
                <h2 id="order-summary-heading" class="sr-only">Order summary</h2>
                <div><span class="summary-label">Placed</span><strong>${orderDetails.order.createdAt}</strong></div>
                <div><span class="summary-label">Status</span><strong class="order-status status-${fn:toLowerCase(orderDetails.order.status)}"><span class="status-dot" aria-hidden="true"></span>${orderDetails.order.status}</strong></div>
                <div><span class="summary-label">Total</span><strong><fmt:formatNumber value="${orderDetails.order.totalAmount}" type="currency" currencyCode="USD" /></strong></div>
            </section>
            <section class="order-items-card" aria-labelledby="order-items-heading">
                <div class="order-items-heading"><h2 id="order-items-heading">Items in this order</h2><span>${fn:length(orderDetails.items)} item<c:if test="${fn:length(orderDetails.items) != 1}">s</c:if></span></div>
                <c:forEach items="${orderDetails.items}" var="item">
                    <article class="history-item">
                        <div class="history-item-image">
                            <c:choose>
                                <c:when test="${not empty item.imageUrl}"><img src="${item.imageUrl}" alt="${item.productName}"></c:when>
                                <c:otherwise><span class="image-placeholder">Ashok<span>Mart</span></span></c:otherwise>
                            </c:choose>
                        </div>
                        <div class="history-item-copy"><h3>${item.productName}</h3><p>${item.quantity} × <fmt:formatNumber value="${item.unitPrice}" type="currency" currencyCode="USD" /> at purchase</p></div>
                        <strong><fmt:formatNumber value="${item.subtotal}" type="currency" currencyCode="USD" /></strong>
                    </article>
                </c:forEach>
                <div class="history-total"><span>Order total</span><strong><fmt:formatNumber value="${orderDetails.order.totalAmount}" type="currency" currencyCode="USD" /></strong></div>
            </section>
            <div class="order-page-actions"><a class="primary-button" href="${pageContext.request.contextPath}/products">Continue shopping</a><a class="secondary-button" href="${pageContext.request.contextPath}/orders">Back to orders</a></div>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
