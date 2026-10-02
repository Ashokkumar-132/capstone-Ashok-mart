<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/" aria-label="AshokMart home">Ashok<span>Mart</span></a>
    <form class="header-search" action="${pageContext.request.contextPath}/products" method="get" role="search">
        <label class="sr-only" for="site-search">Search products</label>
        <input id="site-search" name="q" type="search" placeholder="Search products" maxlength="100">
        <button type="submit" aria-label="Submit product search">Search</button>
    </form>
    <nav class="site-nav" aria-label="Primary navigation">
        <a href="${pageContext.request.contextPath}/products">Shop</a>
        <c:choose>
            <c:when test="${not empty sessionScope.authenticatedUser}">
                <c:if test="${sessionScope.authenticatedUser.role == 'BUYER'}">
                    <a href="${pageContext.request.contextPath}/cart">Cart</a>
                    <a href="${pageContext.request.contextPath}/orders">Orders</a>
                </c:if>
                <c:if test="${sessionScope.authenticatedUser.role == 'SELLER'}">
                    <a href="${pageContext.request.contextPath}/seller/dashboard">Dashboard</a>
                    <a href="${pageContext.request.contextPath}/seller/products">Products</a>
                    <a href="${pageContext.request.contextPath}/seller/orders">Orders</a>
                </c:if>
                <c:if test="${sessionScope.authenticatedUser.role == 'ADMIN'}">
                    <a href="${pageContext.request.contextPath}/admin/dashboard">Dashboard</a>
                    <a href="${pageContext.request.contextPath}/admin/users">Users</a>
                    <a href="${pageContext.request.contextPath}/admin/products">Products</a>
                    <a href="${pageContext.request.contextPath}/admin/orders">Orders</a>
                </c:if>
                <span class="account-label">Hi, <c:out value="${sessionScope.authenticatedUser.name}"/></span>
                <form action="${pageContext.request.contextPath}/logout" method="post" class="inline-form"><button type="submit" class="nav-button">Log out</button></form>
            </c:when>
            <c:otherwise>
                <a href="${pageContext.request.contextPath}/login">Log in</a>
                <a class="nav-cta" href="${pageContext.request.contextPath}/register">Register</a>
            </c:otherwise>
        </c:choose>
    </nav>
</header>
