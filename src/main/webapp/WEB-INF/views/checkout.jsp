<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Checkout | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/checkout.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout checkout-layout">
    <div class="checkout-heading">
        <div><p class="eyebrow">Almost yours</p><h1>Review your order</h1></div>
        <a class="back-link" href="${pageContext.request.contextPath}/cart">← Back to cart</a>
    </div>

    <c:if test="${not empty checkoutError}"><div class="catalog-message error" role="alert">${checkoutError}</div></c:if>

    <c:choose>
        <c:when test="${empty cart or cart.empty}">
            <section class="empty-state" aria-labelledby="empty-checkout-heading">
                <span class="empty-mark" aria-hidden="true">—</span>
                <h2 id="empty-checkout-heading">There is nothing to checkout.</h2>
                <p>Your cart is empty. Add a product before placing an order.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/products">Continue shopping</a>
            </section>
        </c:when>
        <c:otherwise>
            <div class="checkout-columns">
                <section class="checkout-items" aria-labelledby="review-items-heading">
                    <h2 id="review-items-heading">Order items</h2>
                    <c:forEach items="${cart.items}" var="item">
                        <article class="checkout-item">
                            <div class="checkout-item-image">
                                <c:choose>
                                    <c:when test="${not empty item.imageUrl}"><img src="${item.imageUrl}" alt="${item.productName}"></c:when>
                                    <c:otherwise><span class="image-placeholder">Ashok<span>Mart</span></span></c:otherwise>
                                </c:choose>
                            </div>
                            <div class="checkout-item-copy">
                                <h3>${item.productName}</h3>
                                <p>Quantity ${item.quantity} · <fmt:formatNumber value="${item.currentPrice}" type="currency" currencyCode="USD" /> each</p>
                            </div>
                            <strong><fmt:formatNumber value="${item.lineTotal}" type="currency" currencyCode="USD" /></strong>
                        </article>
                    </c:forEach>
                </section>
                <aside class="checkout-summary" aria-labelledby="checkout-summary-heading">
                    <h2 id="checkout-summary-heading">Order summary</h2>
                    <div class="summary-row"><span>Items</span><span>${cart.itemCount}</span></div>
                    <div class="summary-row total-row"><strong>Total</strong><strong><fmt:formatNumber value="${cart.subtotal}" type="currency" currencyCode="USD" /></strong></div>
                    <form action="${pageContext.request.contextPath}/checkout" method="post" onsubmit="this.querySelector('button').disabled=true;">
                        <button class="primary-button" type="submit">Place order</button>
                    </form>
                    <p class="checkout-note">Your total and prices are recalculated from the database when you place the order.</p>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
