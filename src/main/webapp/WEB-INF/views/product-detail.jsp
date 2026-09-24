<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><c:choose><c:when test="${productNotFound}">Product not found</c:when><c:otherwise>${product.name} | AshokMart</c:otherwise></c:choose></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout detail-layout">
    <c:choose>
        <c:when test="${productNotFound}">
            <section class="empty-state detail-empty" aria-labelledby="not-found-heading">
                <p class="eyebrow">Product unavailable</p>
                <h1 id="not-found-heading">We could not find that product.</h1>
                <p>The product may have been removed or is not currently available in the marketplace.</p>
                <a class="secondary-button" href="${pageContext.request.contextPath}/products">Back to products</a>
            </section>
        </c:when>
        <c:otherwise>
            <a class="back-link" href="${pageContext.request.contextPath}/products">← Back to products</a>
            <section class="detail-card" aria-labelledby="product-heading">
                <div class="detail-image">
                    <c:choose>
                        <c:when test="${not empty product.imageUrl}"><img src="${product.imageUrl}" alt="${product.name}"></c:when>
                        <c:otherwise><span class="image-placeholder large-placeholder">Ashok<span>Mart</span></span></c:otherwise>
                    </c:choose>
                </div>
                <div class="detail-content">
                    <p class="product-category">${product.categoryName}</p>
                    <h1 id="product-heading">${product.name}</h1>
                    <p class="detail-price"><fmt:formatNumber value="${product.price}" type="currency" currencyCode="USD" /></p>
                    <p class="detail-description"><c:out value="${product.description}" default="No description provided." /></p>
                    <dl class="detail-facts">
                        <div><dt>Seller</dt><dd>${product.sellerName}</dd></div>
                        <div><dt>Availability</dt><dd><c:choose><c:when test="${product.inStock}">In stock · ${product.stockQuantity} available</c:when><c:otherwise>Out of stock</c:otherwise></c:choose></dd></div>
                    </dl>
                    <p class="rating-placeholder detail-rating">Ratings coming soon</p>
                    <button class="primary-button add-cart-disabled" type="button" disabled title="Cart is coming in a later release">Add to cart <span>(coming soon)</span></button>
                </div>
            </section>
        </c:otherwise>
    </c:choose>
</main>
</body>
</html>
