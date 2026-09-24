package com.ashokmart;

import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.SellerOrderDetailsServlet;
import com.ashokmart.servlet.SellerOrderStatusServlet;
import com.ashokmart.servlet.SellerOrdersServlet;
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

class SellerOrderServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession sellerSession;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "seller_order_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        sellerSession = mock(HttpSession.class);
        when(sellerSession.getAttribute("authenticatedUser"))
                .thenReturn(new AuthenticationResult(3, "Seller A", "seller-a@servlet.test", UserRole.SELLER));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void sellerOrdersListForwardsRelevantOrders() throws Exception {
        SellerOrdersServlet servlet = spy(new SellerOrdersServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/seller/orders", sellerSession);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/seller-orders.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("orders"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void sellerCannotViewOrderWithOnlyAnotherSellersProducts() throws Exception {
        SellerOrderDetailsServlet servlet = spy(new SellerOrderDetailsServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/seller/orders/view", sellerSession);
        when(request.getParameter("id")).thenReturn("2");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/seller-order-details.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(request).setAttribute("sellerOrderNotFound", true);
        verify(dispatcher).forward(request, response);
    }

    @Test
    void statusServletUsesSessionSellerAndRedirectsAfterPost() throws Exception {
        SellerOrderStatusServlet servlet = spy(new SellerOrderStatusServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = request("/app/seller/orders/status", sellerSession);
        when(request.getParameter("orderId")).thenReturn("1");
        when(request.getParameter("status")).thenReturn("CONFIRMED");
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/app/seller/orders");
        org.junit.jupiter.api.Assertions.assertEquals("CONFIRMED", new OrderDaoImpl(pool).findOrderForSeller(1, 3).orElseThrow().getStatus());
    }

    @Test
    void unauthenticatedSellerOrdersRedirectToLogin() throws Exception {
        SellerOrdersServlet servlet = new SellerOrdersServlet();
        HttpServletRequest request = request("/app/seller/orders", null);
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
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer', 'buyer@servlet.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (3, 'Seller A', 'seller-a@servlet.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (4, 'Seller B', 'seller-b@servlet.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 3, 1, 'Seller A product', 10.00, 4, TRUE)");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (2, 4, 1, 'Seller B product', 10.00, 4, TRUE)");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (1, 1, 20.00, 'PENDING')");
            execute(connection, "INSERT INTO orders (id, buyer_id, total_amount, status) VALUES (2, 1, 10.00, 'CONFIRMED')");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (1, 1, 1, 3, 1, 20.00, 20.00)");
            execute(connection, "INSERT INTO order_items (id, order_id, product_id, seller_id, quantity, unit_price, subtotal) VALUES (2, 2, 2, 4, 1, 10.00, 10.00)");
        }
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
