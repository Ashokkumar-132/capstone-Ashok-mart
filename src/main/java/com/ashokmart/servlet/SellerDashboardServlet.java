package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.Product;
import com.ashokmart.service.ProductService;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = "/seller/dashboard")
public final class SellerDashboardServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerDashboardServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        try {
            ProductService service = SellerWebSupport.productService(SellerWebSupport.pool(getServletContext()));
            List<Product> products = service.findAllSellerProducts(seller.userId());
            request.setAttribute("products", products);
            request.setAttribute("productCount", products.size());
            request.setAttribute("activeProductCount", products.stream().filter(Product::isActive).count());
            request.setAttribute("outOfStockCount", products.stream().filter(product -> product.getStockQuantity() == 0).count());
            request.setAttribute("relevantOrderCount", SellerWebSupport.sellerOrderService(SellerWebSupport.pool(getServletContext()))
                    .getOrdersForSeller(seller.userId()).size());
            request.getRequestDispatcher("/WEB-INF/views/seller-dashboard.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller dashboard request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Seller dashboard is temporarily unavailable.");
        }
    }
}
