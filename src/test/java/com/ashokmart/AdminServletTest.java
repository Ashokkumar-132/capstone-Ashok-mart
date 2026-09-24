package com.ashokmart;

import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.AdminDashboardServlet;
import com.ashokmart.servlet.AdminUserStatusServlet;
import com.ashokmart.servlet.AdminUsersServlet;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession adminSession;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "admin_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        adminSession = mock(HttpSession.class);
        when(adminSession.getAttribute("authenticatedUser"))
                .thenReturn(new AuthenticationResult(1, "Admin", "admin@servlet.test", UserRole.ADMIN));
    }

    @AfterEach
    void tearDown() { pool.close(); }

    @Test
    void adminDashboardLoadsRealStatistics() throws Exception {
        AdminDashboardServlet servlet = spy(new AdminDashboardServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/admin/dashboard", adminSession);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("statistics"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void adminUsersLoadsFilterableList() throws Exception {
        AdminUsersServlet servlet = spy(new AdminUsersServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/admin/users", adminSession);
        when(request.getParameter("q")).thenReturn("buyer");
        when(request.getParameter("role")).thenReturn("BUYER");
        when(request.getParameter("status")).thenReturn("ACTIVE");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/admin-users.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("userPage"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void statusEndpointUsesAuthenticatedAdminAndRedirects() throws Exception {
        AdminUserStatusServlet servlet = spy(new AdminUserStatusServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/admin/users/deactivate", adminSession);
        when(request.getServletPath()).thenReturn("/admin/users/deactivate");
        when(request.getParameter("userId")).thenReturn("2");
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/app/admin/users");
        org.junit.jupiter.api.Assertions.assertFalse(new UserDaoImpl(pool).findById(2).orElseThrow().isEnabled());
    }

    @Test
    void unauthenticatedAdminRouteRedirectsToLogin() throws Exception {
        AdminDashboardServlet servlet = new AdminDashboardServlet();
        HttpServletRequest request = request("/app/admin/dashboard", null);
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/app/login");
    }

    private HttpServletRequest request(String uri, HttpSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        return request;
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (1, 'Admin', 'admin@servlet.test', 'hash', 'ADMIN', TRUE)");
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (2, 'Buyer', 'buyer@servlet.test', 'hash', 'BUYER', TRUE)");
        }
    }

    private void insert(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.executeUpdate(); }
    }
}
