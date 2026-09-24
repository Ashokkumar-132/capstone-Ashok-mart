package com.ashokmart.servlet;

import com.ashokmart.dao.impl.CartDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.CartView;
import com.ashokmart.service.CartService;
import com.ashokmart.service.CartValidationException;
import com.ashokmart.service.impl.CartServiceImpl;
import com.ashokmart.service.impl.ProductServiceImpl;
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

@WebServlet(urlPatterns = "/cart/*")
public final class CartServlet extends HttpServlet {
    public static final String FLASH_SUCCESS = "cartFlashSuccess";
    public static final String FLASH_ERROR = "cartFlashError";
    private static final Logger LOGGER = LoggerFactory.getLogger(CartServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult user = authenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        try {
            CartView cart = service(request).getCart(user.userId());
            request.setAttribute("cart", cart);
            copyFlash(request, "cartSuccess", FLASH_SUCCESS);
            copyFlash(request, "cartError", FLASH_ERROR);
            request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Cart view failed", exception);
            request.setAttribute("cartError", "Your cart is temporarily unavailable. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/cart.jsp").forward(request, response);
        }
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        AuthenticationResult user = authenticatedUser(request);
        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String route = request.getRequestURI().substring(request.getContextPath().length());
        try {
            CartService cartService = service(request);
            switch (route) {
                case "/cart/add" -> {
                    cartService.addToCart(user.userId(), parseLong(request.getParameter("productId")), parseInt(request.getParameter("quantity")));
                    flash(request, FLASH_SUCCESS, "Product added to your cart.");
                }
                case "/cart/update" -> {
                    cartService.updateQuantity(user.userId(), parseLong(request.getParameter("productId")), parseInt(request.getParameter("quantity")));
                    flash(request, FLASH_SUCCESS, "Cart updated.");
                }
                case "/cart/remove" -> {
                    cartService.removeItem(user.userId(), parseLong(request.getParameter("productId")));
                    flash(request, FLASH_SUCCESS, "Item removed from your cart.");
                }
                case "/cart/clear" -> {
                    cartService.clearCart(user.userId());
                    flash(request, FLASH_SUCCESS, "Your cart has been cleared.");
                }
                default -> {
                    response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                    return;
                }
            }
        } catch (CartValidationException exception) {
            flash(request, FLASH_ERROR, exception.getMessage());
        } catch (IllegalStateException exception) {
            LOGGER.error("Cart mutation failed", exception);
            flash(request, FLASH_ERROR, "We could not update your cart. Please try again.");
        }
        response.sendRedirect(request.getContextPath() + "/cart");
    }

    private CartService service(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Cart database is unavailable");
        }
        return new CartServiceImpl(new CartDaoImpl(pool), new ProductServiceImpl(new ProductDaoImpl(pool)));
    }

    private AuthenticationResult authenticatedUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        return value instanceof AuthenticationResult result ? result : null;
    }

    private long parseLong(String value) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            throw new CartValidationException("A valid product is required");
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new CartValidationException("A valid quantity is required");
        }
    }

    private void flash(HttpServletRequest request, String key, String message) {
        request.getSession(true).setAttribute(key, message == null ? "Cart request could not be completed" : message);
    }

    private void copyFlash(HttpServletRequest request, String requestKey, String sessionKey) {
        HttpSession session = request.getSession(false);
        if (session == null) return;
        Object message = session.getAttribute(sessionKey);
        if (message != null) {
            request.setAttribute(requestKey, message);
            session.removeAttribute(sessionKey);
        }
    }
}
