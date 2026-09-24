package com.ashokmart;

import com.ashokmart.dao.CartDao;
import com.ashokmart.dao.impl.CartDaoImpl;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.impl.CheckoutServiceImpl;
import com.ashokmart.servlet.CheckoutServlet;
import com.ashokmart.servlet.OrderConfirmationServlet;
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

class CheckoutServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession session;
    private CartDao cartDao;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "checkout_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Buyer', 'buyer@servlet-checkout.test', 'hash', 'BUYER')");
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (2, 'Seller', 'seller@servlet-checkout.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'General')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity, enabled) VALUES (1, 2, 1, 'Checkout Phone', 10.00, 4, TRUE)");
        }
        cartDao = new CartDaoImpl(pool);
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        session = mock(HttpSession.class);
        when(session.getAttribute("authenticatedUser")).thenReturn(new AuthenticationResult(1, "Buyer", "buyer@servlet-checkout.test", UserRole.BUYER));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void checkoutGetForwardsServerCartReview() throws Exception {
        cartDao.addItem(1, 1, 1);
        CheckoutServlet servlet = spy(new CheckoutServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/checkout");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/checkout.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("cart"), any());
        verify(dispatcher).forward(request, response);
    }

    @Test
    void checkoutPostCreatesOrderAndRedirectsToDatabaseBackedConfirmation() throws Exception {
        cartDao.addItem(1, 1, 1);
        CheckoutServlet servlet = spy(new CheckoutServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/checkout");
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doPost(request, response);

        verify(response).sendRedirect("/app/order-confirmation?id=1");
    }

    @Test
    void emptyCheckoutForwardsSafeValidationMessage() throws Exception {
        CheckoutServlet servlet = spy(new CheckoutServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/checkout");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/checkout.jsp")).thenReturn(dispatcher);

        servlet.doPost(request, response);

        verify(request).setAttribute("checkoutError", "Your cart is empty.");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void confirmationServletReloadsOrderAndItemsForAuthenticatedBuyer() throws Exception {
        cartDao.addItem(1, 1, 2);
        long orderId = new CheckoutServiceImpl(pool, new OrderDaoImpl(pool)).checkout(1).getId();
        OrderConfirmationServlet servlet = spy(new OrderConfirmationServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = authenticatedRequest("/app/order-confirmation");
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getParameter("id")).thenReturn(Long.toString(orderId));
        when(request.getRequestDispatcher("/WEB-INF/views/order-confirmation.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("order"), any());
        verify(request).setAttribute(eq("orderItems"), any());
        verify(dispatcher).forward(request, response);
    }

    private HttpServletRequest authenticatedRequest(String uri) {
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
