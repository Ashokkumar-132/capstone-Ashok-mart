package com.ashokmart.filter;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.LoginServlet;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthorizationFilterTest {
    @Test
    void publicRoutePassesThroughAuthenticationFilter() throws Exception {
        AuthenticationFilter filter = new AuthenticationFilter();
        HttpServletRequest request = request("/", null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void guestIsRedirectedFromProtectedRoutes() throws Exception {
        AuthenticationFilter filter = new AuthenticationFilter();
        HttpServletRequest request = request("/seller/products", null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(response).sendRedirect("/login");
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void malformedSessionDoesNotAuthenticateGuest() throws Exception {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE)).thenReturn("not-authentication-data");
        HttpServletRequest request = request("/admin/users", session);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new AuthenticationFilter().doFilter(request, response, chain);

        verify(response).sendRedirect("/login");
        verify(chain, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class));
    }

    @Test
    void buyerCanAccessBuyerRouteButNotSellerOrAdmin() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter();
        AuthenticationResult buyer = user(UserRole.BUYER);
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse allowedResponse = mock(HttpServletResponse.class);
        filter.doFilter(request("/buyer/account", session(buyer)), allowedResponse, chain);
        verify(chain).doFilter(any(ServletRequest.class), eq(allowedResponse));
        filter.doFilter(request("/checkout", session(buyer)), allowedResponse, chain);
        filter.doFilter(request("/orders", session(buyer)), allowedResponse, chain);

        assertForbidden(filter, "/seller/products", buyer);
        assertForbidden(filter, "/admin/users", buyer);
    }

    @Test
    void sellerCanAccessSellerRouteButNotAdmin() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        filter.doFilter(request("/seller/products", session(user(UserRole.SELLER))), response, chain);
        verify(chain).doFilter(any(ServletRequest.class), eq(response));
        assertForbidden(filter, "/admin/users", user(UserRole.SELLER));
        assertForbidden(filter, "/checkout", user(UserRole.SELLER));
        assertForbidden(filter, "/orders", user(UserRole.SELLER));
    }

    @Test
    void adminCanAccessAdminRouteWithoutImplicitSellerOrBuyerHierarchy() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        filter.doFilter(request("/admin/users", session(user(UserRole.ADMIN))), response, chain);
        verify(chain).doFilter(any(ServletRequest.class), eq(response));
        assertForbidden(filter, "/seller/products", user(UserRole.ADMIN));
        assertForbidden(filter, "/buyer/account", user(UserRole.ADMIN));
    }

    @Test
    void onlyBuyersCanAccessCartRoutes() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter();
        FilterChain chain = mock(FilterChain.class);
        HttpServletResponse buyerResponse = mock(HttpServletResponse.class);
        filter.doFilter(request("/cart", session(user(UserRole.BUYER))), buyerResponse, chain);
        verify(chain).doFilter(any(ServletRequest.class), eq(buyerResponse));
        assertForbidden(filter, "/cart/add", user(UserRole.SELLER));
    }

    @Test
    void authorizationRedirectsGuestAndForwardsForbiddenUsersToSafePage() throws Exception {
        AuthorizationFilter filter = new AuthorizationFilter();
        HttpServletRequest guestRequest = request("/admin/users", null);
        HttpServletResponse guestResponse = mock(HttpServletResponse.class);
        filter.doFilter(guestRequest, guestResponse, mock(FilterChain.class));
        verify(guestResponse).sendRedirect("/login");
    }

    private void assertForbidden(AuthorizationFilter filter, String path, AuthenticationResult user) throws Exception {
        HttpServletRequest request = request(path, session(user));
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/error/403.jsp")).thenReturn(dispatcher);

        filter.doFilter(request, response, mock(FilterChain.class));

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(dispatcher).forward(request, response);
    }

    private HttpServletRequest request(String path, HttpSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(path);
        when(request.getContextPath()).thenReturn("");
        when(request.getSession(false)).thenReturn(session);
        return request;
    }

    private HttpSession session(AuthenticationResult user) {
        HttpSession session = mock(HttpSession.class);
        when(session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE)).thenReturn(user);
        return session;
    }

    private AuthenticationResult user(UserRole role) {
        return new AuthenticationResult(1L, "Test User", "test@example.com", role);
    }

}
