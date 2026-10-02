package com.ashokmart.filter;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

/** Protects form mutations with a server-generated token stored only in the HTTP session. */
@WebFilter(filterName = "CsrfFilter", urlPatterns = "/*")
public final class CsrfFilter implements Filter {
    public static final String TOKEN_ATTRIBUTE = "csrfToken";
    public static final String TOKEN_PARAMETER = "_csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpSession session = httpRequest.getSession(false);
        if (!"POST".equalsIgnoreCase(httpRequest.getMethod())) {
            if (session == null && isApplicationPage(httpRequest)) session = httpRequest.getSession(true);
            ensureToken(session);
            chain.doFilter(request, response);
            return;
        }
        if (session == null || !validToken(session, httpRequest.getParameter(TOKEN_PARAMETER))) {
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "The request could not be verified.");
            return;
        }
        chain.doFilter(request, response);
    }

    private void ensureToken(HttpSession session) {
        if (session != null && session.getAttribute(TOKEN_ATTRIBUTE) == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            session.setAttribute(TOKEN_ATTRIBUTE, Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
        }
    }

    private boolean validToken(HttpSession session, String submitted) {
        Object expected = session.getAttribute(TOKEN_ATTRIBUTE);
        return expected instanceof String token && submitted != null && token.equals(submitted);
    }

    private boolean isApplicationPage(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return !path.contains(".") && !path.startsWith("/WEB-INF/");
    }
}
