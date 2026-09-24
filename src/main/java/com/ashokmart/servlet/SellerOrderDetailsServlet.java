package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.SellerOrderException;
import com.ashokmart.service.SellerOrderService;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/seller/orders/view")
public final class SellerOrderDetailsServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerOrderDetailsServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        long orderId;
        try {
            orderId = Long.parseLong(request.getParameter("id"));
            if (orderId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            forwardNotFound(request, response);
            return;
        }
        try {
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            SellerOrderService service = SellerWebSupport.sellerOrderService(pool);
            var details = service.getOrderDetailsForSeller(orderId, seller.userId());
            if (details.isEmpty()) {
                forwardNotFound(request, response);
                return;
            }
            request.setAttribute("sellerOrderDetails", details.get());
            request.getRequestDispatcher("/WEB-INF/views/seller-order-details.jsp").forward(request, response);
        } catch (SellerOrderException exception) {
            forwardNotFound(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller order detail request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Seller order details are temporarily unavailable.");
        }
    }

    private void forwardNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("sellerOrderNotFound", true);
        request.getRequestDispatcher("/WEB-INF/views/seller-order-details.jsp").forward(request, response);
    }
}
