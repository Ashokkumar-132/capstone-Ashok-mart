package com.ashokmart.filter;

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

/** Adds conservative headers without imposing a CSP that breaks the current JSP UI. */
@WebFilter(filterName = "SecurityHeadersFilter", urlPatterns = "/*")
public final class SecurityHeadersFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE) != null) {
            httpResponse.setHeader("Cache-Control", "no-store");
        }
        chain.doFilter(request, response);
    }
}
