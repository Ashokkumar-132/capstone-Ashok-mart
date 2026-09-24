<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Ashok<span>Mart</span></a>
    <nav class="site-nav" aria-label="Primary navigation">
        <a href="${pageContext.request.contextPath}/products">Shop</a>
        <c:choose>
            <c:when test="${not empty sessionScope.authenticatedUser}">
                <c:if test="${sessionScope.authenticatedUser.role == 'BUYER'}">
                    <a href="${pageContext.request.contextPath}/cart">Cart</a>
                    <a href="${pageContext.request.contextPath}/orders">My Orders</a>
                </c:if>
                <c:if test="${sessionScope.authenticatedUser.role == 'SELLER'}">
                    <a href="${pageContext.request.contextPath}/seller/dashboard">Seller Dashboard</a>
                    <a href="${pageContext.request.contextPath}/seller/products">My Products</a>
                    <a href="${pageContext.request.contextPath}/seller/orders">Seller Orders</a>
                </c:if>
                <span class="account-label">Hi, ${sessionScope.authenticatedUser.name}</span>
                <form action="${pageContext.request.contextPath}/logout" method="post" class="inline-form">
                    <button type="submit" class="nav-button">Log out</button>
                </form>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/login">Log in</a>
                <a class="nav-cta" href="${pageContext.request.contextPath}/register">Create account</a>
            </c:otherwise>
        </c:choose>
    </nav>
</header>
