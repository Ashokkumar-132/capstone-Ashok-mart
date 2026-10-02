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
                    <p class="detail-rating" aria-label="Product rating"><strong><c:choose><c:when test="${reviewCount > 0}">${averageRating} ★</c:when><c:otherwise>No ratings yet</c:otherwise></c:choose></strong> <span>${reviewCount} review<c:if test="${reviewCount != 1}">s</c:if></span></p>
                    <c:choose>
                        <c:when test="${not product.inStock}">
                            <p class="out-of-stock-note">This product is currently out of stock.</p>
                        </c:when>
                        <c:when test="${sessionScope.authenticatedUser.role == 'BUYER'}">
                            <form action="${pageContext.request.contextPath}/cart/add" method="post" class="detail-cart-form">
                                <input type="hidden" name="productId" value="${product.id}">
                                <label for="detail-quantity">Quantity</label>
                                <div class="detail-cart-actions">
                                    <input id="detail-quantity" name="quantity" type="number" min="1" max="${product.stockQuantity}" value="1" required>
                                    <button class="primary-button" type="submit">Add to cart</button>
                                </div>
                            </form>
                        </c:when>
                        <c:otherwise>
                            <a class="primary-button login-to-cart" href="${pageContext.request.contextPath}/login">Log in to add to cart</a>
                        </c:otherwise>
                    </c:choose>
                </div>
            </section>
            <section class="reviews-panel" aria-labelledby="reviews-heading">
                <div class="reviews-heading"><div><p class="eyebrow">Customer feedback</p><h2 id="reviews-heading">Reviews</h2></div><span class="review-summary"><strong>${averageRating}</strong> ★ · ${reviewCount} total</span></div>
                <c:if test="${not empty reviewSuccess}"><p class="flash-success">${reviewSuccess}</p></c:if><c:if test="${not empty reviewError}"><p class="flash-error">${reviewError}</p></c:if>
                <c:choose><c:when test="${empty reviews}"><p class="review-empty">No reviews yet. Be the first eligible buyer to share your experience.</p></c:when><c:otherwise><div class="review-list"><c:forEach items="${reviews}" var="review"><article class="review-card"><div class="review-card-heading"><strong><c:out value="${review.buyerName}"/></strong><span>${review.rating} ★ · ${review.createdAt}</span></div><p><c:out value="${review.comment}" default="No written comment."/></p><c:if test="${not empty sessionScope.authenticatedUser && sessionScope.authenticatedUser.role == 'BUYER' && buyerReview != null && buyerReview.id == review.id}"><span class="review-owner">Your review</span></c:if></article></c:forEach></div></c:otherwise></c:choose>
                <c:if test="${canReview}"><form class="review-form" method="post" action="${pageContext.request.contextPath}/reviews/create"><h3>Share your experience</h3><input type="hidden" name="productId" value="${product.id}"><label for="review-rating">Rating<select id="review-rating" name="rating" required><option value="">Choose a rating</option><option value="5">5 — Excellent</option><option value="4">4 — Good</option><option value="3">3 — Average</option><option value="2">2 — Poor</option><option value="1">1 — Very poor</option></select></label><label for="review-comment">Comment<textarea id="review-comment" name="comment" maxlength="4000" rows="4" placeholder="What should other shoppers know?"></textarea></label><button class="primary-button" type="submit">Publish review</button></form></c:if>
                <c:if test="${buyerReview != null}"><form class="review-form" method="post" action="${pageContext.request.contextPath}/reviews/update"><h3>Edit your review</h3><input type="hidden" name="reviewId" value="${buyerReview.id}"><label for="edit-review-rating">Rating<select id="edit-review-rating" name="rating" required><c:forEach begin="1" end="5" var="rating"><option value="${rating}" <c:if test="${buyerReview.rating == rating}">selected</c:if>>${rating} ★</option></c:forEach></select></label><label for="edit-review-comment">Comment<textarea id="edit-review-comment" name="comment" maxlength="4000" rows="4"><c:out value="${buyerReview.comment}"/></textarea></label><button class="secondary-button" type="submit">Save changes</button></form><form method="post" action="${pageContext.request.contextPath}/reviews/delete" class="review-delete-form"><input type="hidden" name="reviewId" value="${buyerReview.id}"><button class="link-button danger" type="submit">Delete your review</button></form></c:if>
                <c:if test="${empty sessionScope.authenticatedUser}"><p class="review-login-note"><a href="${pageContext.request.contextPath}/login">Log in</a> as a buyer to review products you have purchased.</p></c:if>
            </section>
        </c:otherwise>
    </c:choose>
</main>
<jsp:include page="/WEB-INF/views/includes/footer.jsp" />
</body>
</html>
