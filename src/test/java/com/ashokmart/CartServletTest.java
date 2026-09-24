package com.ashokmart;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.CartServlet;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "cart_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer', 'buyer@servlet-cart.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (2, 'Seller', 'seller@servlet-cart.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 2, 1, 'Cart Phone', 10.00, 5, TRUE)");
        }
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        session = mock(HttpSession.class);
        when(session.getAttribute("authenticatedUser")).thenReturn(new AuthenticationResult(1, "Buyer", "buyer@servlet-cart.test", UserRole.BUYER));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void addPostUsesAuthenticatedSessionAndRedirectsToCart() throws Exception {
        CartServlet servlet = spy(new CartServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/cart/add");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("productId")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("2");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/app/cart");
        verify(session).setAttribute(CartServlet.FLASH_SUCCESS, "Product added to your cart.");
    }

    @Test
    void invalidMutationUsesFlashErrorAndStillUsesPrg() throws Exception {
        CartServlet servlet = spy(new CartServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/cart/update");
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getParameter("productId")).thenReturn("1");
        when(request.getParameter("quantity")).thenReturn("0");

        servlet.doPost(request, response);

        verify(response).sendRedirect("/app/cart");
        verify(session).setAttribute(CartServlet.FLASH_ERROR, "Quantity must be at least 1");
    }

    @Test
    void getForwardsServerCartViewAndUnauthenticatedAccessRedirectsToLogin() throws Exception {
        CartServlet servlet = spy(new CartServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/cart");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/cart.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);
        verify(request).setAttribute(eq("cart"), org.mockito.ArgumentMatchers.any());
        verify(dispatcher).forward(request, response);

        HttpServletRequest guestRequest = mock(HttpServletRequest.class);
        when(guestRequest.getContextPath()).thenReturn("/app");
        when(guestRequest.getSession(false)).thenReturn(null);
        servlet.doGet(guestRequest, response);
        verify(response).sendRedirect("/app/login");
    }

    private HttpServletRequest authenticatedRequest(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        return request;
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
