<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Seller Dashboard | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css"><link rel="stylesheet" href="${pageContext.request.contextPath}/css/seller.css">
</head>
<body><jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="seller-layout">
    <section class="seller-hero"><div><p class="eyebrow">Seller workspace</p><h1>Build your storefront.</h1><p class="lede">Welcome back, ${sessionScope.authenticatedUser.name}. Keep your catalog fresh and your customers moving.</p></div><a class="primary-button seller-hero-action" href="${pageContext.request.contextPath}/seller/products/new">+ Add product</a></section>
    <section class="seller-stat-grid" aria-label="Seller summary"><article class="seller-stat"><span>Total products</span><strong>${productCount}</strong></article><article class="seller-stat"><span>Active products</span><strong>${activeProductCount}</strong></article><article class="seller-stat"><span>Out of stock</span><strong>${outOfStockCount}</strong></article><article class="seller-stat"><span>Relevant orders</span><strong>${relevantOrderCount}</strong></article></section>
    <section class="seller-panel"><div class="seller-panel-heading"><div><p class="eyebrow">Your catalog</p><h2>Product management</h2></div><a class="secondary-button" href="${pageContext.request.contextPath}/seller/products">View all products</a></div>
        <c:choose><c:when test="${empty products}"><div class="seller-empty"><h3>Your storefront is empty.</h3><p>Create your first product to begin selling on AshokMart.</p><a class="secondary-button" href="${pageContext.request.contextPath}/seller/products/new">Create first product</a></div></c:when><c:otherwise><div class="seller-preview-list"><c:forEach items="${products}" var="product" end="4"><div class="seller-preview-row"><div class="seller-thumb"><c:choose><c:when test="${not empty product.imageUrl}"><img src="${product.imageUrl}" alt="${product.name}"></c:when><c:otherwise><span>AM</span></c:otherwise></c:choose></div><div><strong>${product.name}</strong><span>${product.stockQuantity} in stock · <fmt:formatNumber value="${product.price}" type="currency" currencyCode="USD" /></span></div><span class="seller-state ${product.active ? 'active' : 'inactive'}">${product.active ? 'Active' : 'Inactive'}</span><a class="text-button" href="${pageContext.request.contextPath}/seller/products/edit?id=${product.id}">Edit</a></div></c:forEach></div></c:otherwise></c:choose>
    </section>
</main></body></html>
