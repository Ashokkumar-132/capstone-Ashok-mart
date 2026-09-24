package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.SellerOrderService;
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

@WebServlet(urlPatterns = "/seller/orders")
public final class SellerOrdersServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerOrdersServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        try {
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            SellerOrderService service = SellerWebSupport.sellerOrderService(pool);
            request.setAttribute("orders", service.getOrdersForSeller(seller.userId()));
            HttpSession session = request.getSession(false);
            if (session != null) {
                moveFlash(session, request, "sellerOrderSuccess");
                moveFlash(session, request, "sellerOrderError");
            }
            request.getRequestDispatcher("/WEB-INF/views/seller-orders.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller order list request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Seller orders are temporarily unavailable.");
        }
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object value = session.getAttribute(key);
        if (value != null) {
            request.setAttribute(key, value);
            session.removeAttribute(key);
        }
    }
}
