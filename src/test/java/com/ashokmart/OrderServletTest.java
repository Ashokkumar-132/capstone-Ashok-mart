package com.ashokmart;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.OrderDetailsServlet;
import com.ashokmart.servlet.OrdersServlet;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class OrderServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession buyerSession;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "order_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer', 'buyer@order-servlet.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (2, 'Other Buyer', 'other@order-servlet.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller', 'seller@order-servlet.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 3, 1, 'Phone', 10.00, 5, TRUE)");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (1, 1, 10.00, 'CONFIRMED')");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (2, 2, 10.00, 'DELIVERED')");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (1, 1, 1, 3, 1, 10.00, 10.00)");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (2, 2, 1, 3, 1, 10.00, 10.00)");
        }
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        buyerSession = mock(HttpSession.class);
        when(buyerSession.getAttribute("authenticatedUser"))
                .thenReturn(new AuthenticationResult(1, "Buyer", "buyer@order-servlet.test", UserRole.BUYER));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void authenticatedBuyerCanViewOrders() throws Exception {
        OrdersServlet servlet = spy(new OrdersServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/orders", buyerSession);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/orders.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("orders"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void unauthenticatedOrdersAccessRedirectsToLogin() throws Exception {
        OrdersServlet servlet = new OrdersServlet();
        HttpServletRequest request = request("/app/orders", null);
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/app/login");
    }

    @Test
    void buyerCanViewOwnedDetailsWithStoredPrice() throws Exception {
        OrderDetailsServlet servlet = spy(new OrderDetailsServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/orders/view", buyerSession);
        when(request.getParameter("id")).thenReturn("1");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/order-details.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("orderDetails"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void buyerCannotViewAnotherBuyersOrderByChangingId() throws Exception {
        OrderDetailsServlet servlet = spy(new OrderDetailsServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/orders/view", buyerSession);
        when(request.getParameter("id")).thenReturn("2");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/order-details.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(request).setAttribute("orderNotFound", true);
        verify(dispatcher).forward(request, response);
        verify(request, never()).setAttribute(eq("orderDetails"), any());
    }

    private HttpServletRequest request(String uri, HttpSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getSession(false)).thenReturn(session);
        return request;
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
