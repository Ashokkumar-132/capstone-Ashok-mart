<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<footer class="site-footer">
    <div class="site-footer-inner">
        <div><a class="brand" href="${pageContext.request.contextPath}/">Ashok<span>Mart</span></a><p>Choice, quality, and a marketplace built for real people.</p></div>
        <div class="footer-links"><a href="${pageContext.request.contextPath}/products">Shop products</a><c:if test="${not empty sessionScope.authenticatedUser && sessionScope.authenticatedUser.role == 'BUYER'}"><a href="${pageContext.request.contextPath}/orders">My orders</a></c:if><a href="${pageContext.request.contextPath}/login">Account</a></div>
    </div>
    <div class="site-footer-bottom"><span>© 2026 AshokMart</span><span>Built with care for independent sellers and buyers.</span></div>
</footer>
