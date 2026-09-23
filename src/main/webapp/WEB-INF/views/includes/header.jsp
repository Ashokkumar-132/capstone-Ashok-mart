<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<header class="site-header">
    <a class="brand" href="${pageContext.request.contextPath}/">Ashok<span>Mart</span></a>
    <nav class="site-nav" aria-label="Primary navigation">
        <c:choose>
            <c:when test="${not empty sessionScope.authenticatedUser}">
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
