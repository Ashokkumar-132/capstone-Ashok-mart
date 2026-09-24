package com.ashokmart.servlet;

import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.OrderService;
import com.ashokmart.service.OrderValidationException;
import com.ashokmart.service.impl.OrderServiceImpl;
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

@WebServlet(urlPatterns = "/orders/view")
public final class OrderDetailsServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderDetailsServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult buyer = authenticatedBuyer(request, response);
        if (buyer == null) return;
        long orderId;
        try {
            orderId = Long.parseLong(request.getParameter("id"));
            if (orderId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            forwardNotFound(request, response);
            return;
        }
        try {
            OrderService service = service(request);
            var details = service.getOrderDetailsForBuyer(orderId, buyer.userId());
            if (details.isEmpty()) {
                forwardNotFound(request, response);
                return;
            }
            request.setAttribute("orderDetails", details.get());
            request.getRequestDispatcher("/WEB-INF/views/order-details.jsp").forward(request, response);
        } catch (OrderValidationException exception) {
            forwardNotFound(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Order details request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Order details are temporarily unavailable.");
        }
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

    private OrderService service(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Order database is unavailable");
        }
        return new OrderServiceImpl(new OrderDaoImpl(pool));
    }

    private void forwardNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("orderNotFound", true);
        request.getRequestDispatcher("/WEB-INF/views/order-details.jsp").forward(request, response);
    }
}
