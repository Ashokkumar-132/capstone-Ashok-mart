package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.ProductService;
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

@WebServlet(urlPatterns = "/seller/products")
public final class SellerProductsServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerProductsServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        try {
            ProductService service = SellerWebSupport.productService(SellerWebSupport.pool(getServletContext()));
            request.setAttribute("products", service.findAllSellerProducts(seller.userId()));
            request.setAttribute("categories", SellerWebSupport.categoryService(SellerWebSupport.pool(getServletContext())).listCategories());
            HttpSession session = request.getSession(false);
            if (session != null) {
                moveFlash(session, request, "sellerSuccess");
                moveFlash(session, request, "sellerError");
            }
            request.getRequestDispatcher("/WEB-INF/views/seller-products.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller product list request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Seller products are temporarily unavailable.");
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
