<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Shop | AshokMart</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/auth.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/catalog.css">
</head>
<body>
<jsp:include page="/WEB-INF/views/includes/header.jsp" />
<main class="catalog-layout">
    <section class="catalog-hero" aria-labelledby="catalog-heading">
        <p class="eyebrow">The AshokMart edit</p>
        <h1 id="catalog-heading">Find something worth keeping.</h1>
        <p class="lede">Explore products from independent sellers, with the details you need to choose well.</p>
    </section>

    <form class="catalog-toolbar" action="${pageContext.request.contextPath}/products" method="get">
        <div class="search-field">
            <label for="search">Search the marketplace</label>
            <input id="search" name="q" type="search" placeholder="Try phones, desks, headphones..." value="${searchTerm}" maxlength="100">
        </div>
        <div class="filter-grid">
            <div class="filter-field">
                <label for="category">Category</label>
                <select id="category" name="category">
                    <option value="">All categories</option>
                    <c:forEach items="${categories}" var="category">
                        <option value="${category.id}" <c:if test="${selectedCategoryId == category.id}">selected</c:if>>${category.name}</option>
                    </c:forEach>
                </select>
            </div>
            <div class="filter-field">
                <label for="minPrice">Minimum price</label>
                <input id="minPrice" name="minPrice" type="number" min="0" step="0.01" inputmode="decimal" value="${minimumPrice}">
            </div>
            <div class="filter-field">
                <label for="maxPrice">Maximum price</label>
                <input id="maxPrice" name="maxPrice" type="number" min="0" step="0.01" inputmode="decimal" value="${maximumPrice}">
            </div>
            <div class="filter-field">
                <label for="sort">Sort by</label>
                <select id="sort" name="sort">
                    <option value="newest" <c:if test="${selectedSort == 'newest'}">selected</c:if>>Newest</option>
                    <option value="price_asc" <c:if test="${selectedSort == 'price_asc'}">selected</c:if>>Price: Low to High</option>
                    <option value="price_desc" <c:if test="${selectedSort == 'price_desc'}">selected</c:if>>Price: High to Low</option>
                    <option value="name_asc" <c:if test="${selectedSort == 'name_asc'}">selected</c:if>>Name: A-Z</option>
                    <option value="name_desc" <c:if test="${selectedSort == 'name_desc'}">selected</c:if>>Name: Z-A</option>
                </select>
            </div>
        </div>
        <div class="filter-actions">
            <label class="checkbox-label"><input type="checkbox" name="stock" value="in_stock" <c:if test="${inStockOnly}">checked</c:if>> In stock only</label>
            <div class="filter-buttons">
                <a class="text-button" href="${pageContext.request.contextPath}/products">Clear filters</a>
                <button class="primary-button compact-button" type="submit">Apply filters</button>
            </div>
        </div>
    </form>

    <c:if test="${not empty catalogError}">
        <div class="catalog-message error" role="alert">${catalogError}</div>
    </c:if>

    <section class="catalog-results" aria-labelledby="results-heading">
        <div class="results-heading">
            <div>
                <p class="eyebrow">Curated for you</p>
                <h2 id="results-heading">Products</h2>
            </div>
            <c:if test="${not empty productPage}">
                <p class="result-count">${productPage.totalResults} result<c:if test="${productPage.totalResults != 1}">s</c:if></p>
            </c:if>
        </div>

        <c:choose>
            <c:when test="${empty productPage or empty productPage.products}">
                <div class="empty-state">
                    <span class="empty-mark" aria-hidden="true">—</span>
                    <h3>No products found.</h3>
                    <p>Try a different search or clear your filters to see more of AshokMart.</p>
                    <a class="secondary-button" href="${pageContext.request.contextPath}/products">View all products</a>
                </div>
            </c:when>
            <c:otherwise>
                <div class="product-grid">
                    <c:forEach items="${productPage.products}" var="product">
                        <article class="product-card">
                            <a class="product-image" href="${pageContext.request.contextPath}/product?id=${product.id}">
                                <c:choose>
                                    <c:when test="${not empty product.imageUrl}">
                                        <img src="${product.imageUrl}" alt="${product.name}">
                                    </c:when>
                                    <c:otherwise>
                                        <span class="image-placeholder" aria-label="No product image">Ashok<span>Mart</span></span>
                                    </c:otherwise>
                                </c:choose>
                            </a>
                            <div class="product-card-body">
                                <p class="product-category">${product.categoryName}</p>
                                <h3><a href="${pageContext.request.contextPath}/product?id=${product.id}">${product.name}</a></h3>
                                <p class="product-description"><c:out value="${product.description}" default="No description provided." /></p>
                                <div class="product-meta">
                                    <span class="product-price"><fmt:formatNumber value="${product.price}" type="currency" currencyCode="USD" /></span>
                                    <c:choose>
                                        <c:when test="${product.inStock}"><span class="stock-badge in-stock">In stock · ${product.stockQuantity}</span></c:when>
                                        <c:otherwise><span class="stock-badge out-stock">Out of stock</span></c:otherwise>
                                    </c:choose>
                                </div>
                                <p class="rating-placeholder">Ratings coming soon</p>
                                <a class="card-link" href="${pageContext.request.contextPath}/product?id=${product.id}">View details <span aria-hidden="true">→</span></a>
                            </div>
                        </article>
                    </c:forEach>
                </div>

                <c:if test="${productPage.totalPages > 1}">
                    <nav class="pagination" aria-label="Product pages">
                        <c:if test="${productPage.currentPage > 1}">
                            <c:url var="previousUrl" value="/products">
                                <c:param name="q" value="${searchTerm}"/><c:param name="category" value="${selectedCategoryId}"/>
                                <c:param name="minPrice" value="${minimumPrice}"/><c:param name="maxPrice" value="${maximumPrice}"/>
                                <c:if test="${inStockOnly}"><c:param name="stock" value="in_stock"/></c:if>
                                <c:param name="sort" value="${selectedSort}"/><c:param name="size" value="${productPage.pageSize}"/>
                                <c:param name="page" value="${productPage.currentPage - 1}"/>
                            </c:url>
                            <a class="page-link" href="${previousUrl}">Previous</a>
                        </c:if>
                        <div class="page-numbers">
                            <c:forEach begin="1" end="${productPage.totalPages}" var="pageNumber">
                                <c:url var="pageUrl" value="/products">
                                    <c:param name="q" value="${searchTerm}"/><c:param name="category" value="${selectedCategoryId}"/>
                                    <c:param name="minPrice" value="${minimumPrice}"/><c:param name="maxPrice" value="${maximumPrice}"/>
                                    <c:if test="${inStockOnly}"><c:param name="stock" value="in_stock"/></c:if>
                                    <c:param name="sort" value="${selectedSort}"/><c:param name="size" value="${productPage.pageSize}"/>
                                    <c:param name="page" value="${pageNumber}"/>
                                </c:url>
                                <a class="page-number <c:if test="${pageNumber == productPage.currentPage}">current</c:if>" href="${pageUrl}" <c:if test="${pageNumber == productPage.currentPage}">aria-current="page"</c:if>>${pageNumber}</a>
                            </c:forEach>
                        </div>
                        <c:if test="${productPage.currentPage < productPage.totalPages}">
                            <c:url var="nextUrl" value="/products">
                                <c:param name="q" value="${searchTerm}"/><c:param name="category" value="${selectedCategoryId}"/>
                                <c:param name="minPrice" value="${minimumPrice}"/><c:param name="maxPrice" value="${maximumPrice}"/>
                                <c:if test="${inStockOnly}"><c:param name="stock" value="in_stock"/></c:if>
                                <c:param name="sort" value="${selectedSort}"/><c:param name="size" value="${productPage.pageSize}"/>
                                <c:param name="page" value="${productPage.currentPage + 1}"/>
                            </c:url>
                            <a class="page-link" href="${nextUrl}">Next</a>
                        </c:if>
                    </nav>
                </c:if>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
