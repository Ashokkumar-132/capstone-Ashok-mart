<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Your Cart | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/cart.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout cart-layout">
    <div class="cart-heading">
        <div>
            <p class="eyebrow">Saved for you</p>
            <h1>Your Cart</h1>
        </div>
        <a class="back-link" href="${pageContext.request.contextPath}/products">← Continue shopping</a>
    </div>

    <c:if test="${not empty cartSuccess}"><div class="catalog-message success" role="status">${cartSuccess}</div></c:if>
    <c:if test="${not empty cartError}"><div class="catalog-message error" role="alert">${cartError}</div></c:if>

    <c:choose>
        <c:when test="${empty cart or cart.empty}">
            <section class="empty-state cart-empty" aria-labelledby="empty-cart-heading">
                <span class="empty-mark" aria-hidden="true">—</span>
                <h2 id="empty-cart-heading">Your cart is empty.</h2>
                <p>Take a look around. Your next good find could be waiting in the marketplace.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/products">Browse products</a>
            </section>
        </c:when>
        <c:otherwise>
            <div class="cart-columns">
                <section class="cart-items" aria-labelledby="cart-items-heading">
                    <h2 id="cart-items-heading" class="sr-only">Cart items</h2>
                    <c:forEach items="${cart.items}" var="item">
                        <article class="cart-item">
                            <div class="cart-item-image">
                                <c:choose>
                                    <c:when test="${not empty item.imageUrl}"><img src="${item.imageUrl}" alt="${item.productName}"></c:when>
                                    <c:otherwise><span class="image-placeholder">Ashok<span>Mart</span></span></c:otherwise>
                                </c:choose>
                            </div>
                            <div class="cart-item-info">
                                <h3>${item.productName}</h3>
                                <p class="cart-unit-price"><fmt:formatNumber value="${item.currentPrice}" type="currency" currencyCode="USD" /> each</p>
                                <c:choose>
                                    <c:when test="${item.active and item.availableStock > 0}">
                                        <p class="cart-availability">${item.availableStock} available</p>
                                    </c:when>
                                    <c:otherwise><p class="cart-availability unavailable">Currently unavailable</p></c:otherwise>
                                </c:choose>
                            </div>
                            <div class="cart-item-controls">
                                <form action="${pageContext.request.contextPath}/cart/update" method="post" class="quantity-form">
                                    <label for="quantity-${item.productId}">Quantity</label>
                                    <input id="quantity-${item.productId}" name="quantity" type="number" min="1" max="${item.availableStock}" value="${item.quantity}" <c:if test="${not item.active or item.availableStock < 1}">disabled</c:if>>
                                    <input type="hidden" name="productId" value="${item.productId}">
                                    <button type="submit" class="text-button" <c:if test="${not item.active or item.availableStock < 1}">disabled</c:if>>Update</button>
                                </form>
                                <form action="${pageContext.request.contextPath}/cart/remove" method="post">
                                    <input type="hidden" name="productId" value="${item.productId}">
                                    <button type="submit" class="remove-button">Remove</button>
                                </form>
                            </div>
                            <strong class="cart-line-total"><fmt:formatNumber value="${item.lineTotal}" type="currency" currencyCode="USD" /></strong>
                        </article>
                    </c:forEach>
                </section>
                <aside class="cart-summary" aria-labelledby="summary-heading">
                    <h2 id="summary-heading">Order summary</h2>
                    <div class="summary-row"><span>Items</span><span>${cart.itemCount}</span></div>
                    <div class="summary-row total-row"><strong>Subtotal</strong><strong><fmt:formatNumber value="${cart.subtotal}" type="currency" currencyCode="USD" /></strong></div>
                    <a class="primary-button checkout-link" href="${pageContext.request.contextPath}/checkout">Proceed to checkout</a>
                    <form action="${pageContext.request.contextPath}/cart/clear" method="post">
                        <button class="clear-cart-button" type="submit">Clear cart</button>
                    </form>
                    <p class="summary-note">Prices are refreshed from the marketplace for every cart request.</p>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
