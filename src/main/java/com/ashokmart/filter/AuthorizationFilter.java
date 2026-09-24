package com.ashokmart.filter;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.LoginServlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/** Enforces explicit, non-hierarchical role rules for protected URL namespaces. */
@WebFilter(filterName = "AuthorizationFilter", urlPatterns = {"/buyer/*", "/seller/*", "/admin/*", "/cart/*"})
public final class AuthorizationFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        AuthenticationResult user = authenticatedUser(httpRequest);
        if (user == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
            return;
        }

        UserRole requiredRole = requiredRole(httpRequest);
        if (requiredRole != null && user.role() == requiredRole) {
            chain.doFilter(request, response);
            return;
        }

        httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
        httpRequest.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(request, response);
    }

    private AuthenticationResult authenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        return value instanceof AuthenticationResult result ? result : null;
    }

    private UserRole requiredRole(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/buyer") || path.startsWith("/buyer/")) {
            return UserRole.BUYER;
        }
        if (path.equals("/seller") || path.startsWith("/seller/")) {
            return UserRole.SELLER;
        }
        if (path.equals("/admin") || path.startsWith("/admin/")) {
            return UserRole.ADMIN;
        }
        if (path.equals("/cart") || path.startsWith("/cart/")) {
            return UserRole.BUYER;
        }
        return null;
    }
}
