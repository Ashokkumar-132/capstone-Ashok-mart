package com.ashokmart.filter;

import com.ashokmart.model.AuthenticationResult;
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

/** Redirects guests away from the protected buyer, seller, and admin namespaces. */
@WebFilter(filterName = "AuthenticationFilter", urlPatterns = {"/buyer/*", "/seller/*", "/admin/*"})
public final class AuthenticationFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!isProtectedPath(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = httpRequest.getSession(false);
        Object authenticatedUser = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        if (authenticatedUser instanceof AuthenticationResult) {
            chain.doFilter(request, response);
            return;
        }

        httpResponse.sendRedirect(httpRequest.getContextPath() + "/login");
    }

    private boolean isProtectedPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return path.equals("/buyer") || path.startsWith("/buyer/")
                || path.equals("/seller") || path.startsWith("/seller/")
                || path.equals("/admin") || path.startsWith("/admin/");
    }
}
