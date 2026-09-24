package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.SellerOrderException;
import com.ashokmart.service.SellerOrderService;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = "/seller/orders/status")
public final class SellerOrderStatusServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerOrderStatusServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        try {
            long orderId = Long.parseLong(request.getParameter("orderId"));
            if (orderId <= 0) throw new NumberFormatException();
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            SellerOrderService service = SellerWebSupport.sellerOrderService(pool);
            service.updateOrderStatusForSeller(orderId, seller.userId(), request.getParameter("status"));
            flash(request, "sellerOrderSuccess", "Order status updated.");
        } catch (NumberFormatException exception) {
            flash(request, "sellerOrderError", "Please provide a valid order.");
        } catch (SellerOrderException exception) {
            flash(request, "sellerOrderError", exception.getMessage());
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller order status request failed", exception);
            flash(request, "sellerOrderError", "Order status could not be updated. Please try again.");
        }
        response.sendRedirect(request.getContextPath() + "/seller/orders");
    }

    private void flash(HttpServletRequest request, String key, String value) {
        HttpSession session = request.getSession(true);
        session.setAttribute(key, value);
    }
}
