package com.ashokmart.servlet;

import com.ashokmart.dao.impl.CartDaoImpl;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.CartView;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.CartService;
import com.ashokmart.service.CheckoutService;
import com.ashokmart.service.CheckoutValidationException;
import com.ashokmart.service.impl.CartServiceImpl;
import com.ashokmart.service.impl.CheckoutServiceImpl;
import com.ashokmart.service.impl.ProductServiceImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = "/checkout")
public final class CheckoutServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(CheckoutServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult buyer = authenticatedBuyer(request, response);
        if (buyer == null) return;
        try {
            request.setAttribute("cart", cartService(request).getCart(buyer.userId()));
            request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Checkout review failed", exception);
            request.setAttribute("checkoutError", "Checkout is temporarily unavailable. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult buyer = authenticatedBuyer(request, response);
        if (buyer == null) return;
        try {
            var order = checkoutService(request).checkout(buyer.userId());
            response.sendRedirect(request.getContextPath() + "/order-confirmation?id=" + order.getId());
        } catch (CheckoutValidationException exception) {
            showCheckoutError(request, response, buyer, exception.getMessage());
        } catch (IllegalStateException exception) {
            LOGGER.error("Checkout placement failed", exception);
            showCheckoutError(request, response, buyer, "Checkout could not be completed. Please try again.");
        }
    }

    private void showCheckoutError(HttpServletRequest request, HttpServletResponse response,
                                   AuthenticationResult buyer, String message) throws ServletException, IOException {
        request.setAttribute("checkoutError", message);
        try {
            request.setAttribute("cart", cartService(request).getCart(buyer.userId()));
        } catch (IllegalStateException exception) {
            LOGGER.error("Could not reload cart after checkout failure", exception);
        }
        request.getRequestDispatcher("/WEB-INF/views/checkout.jsp").forward(request, response);
    }

    private AuthenticationResult authenticatedBuyer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(value instanceof AuthenticationResult buyer)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (buyer.role() != UserRole.BUYER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return buyer;
    }

    private CartService cartService(HttpServletRequest request) {
        DatabaseConnectionPool pool = pool(request);
        return new CartServiceImpl(new CartDaoImpl(pool), new ProductServiceImpl(new ProductDaoImpl(pool)));
    }

    private CheckoutService checkoutService(HttpServletRequest request) {
        DatabaseConnectionPool pool = pool(request);
        return new CheckoutServiceImpl(pool, new OrderDaoImpl(pool));
    }

    private DatabaseConnectionPool pool(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Checkout database is unavailable");
        }
        return pool;
    }
}
