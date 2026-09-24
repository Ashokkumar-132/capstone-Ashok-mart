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
import java.io.IOException;

@WebServlet(urlPatterns = {"/seller/products/new", "/seller/products/edit"})
public final class SellerProductFormServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerProductFormServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        try {
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            request.setAttribute("categories", SellerWebSupport.categoryService(pool).listCategories());
            if (request.getServletPath().endsWith("/edit")) {
                long productId = parseId(request.getParameter("id"));
                ProductService service = SellerWebSupport.productService(pool);
                var product = service.findSellerProduct(seller.userId(), productId);
                if (product.isEmpty()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    request.setAttribute("formNotFound", true);
                } else {
                    request.setAttribute("product", product.get());
                    request.setAttribute("editing", true);
                }
            }
            request.getRequestDispatcher("/WEB-INF/views/seller-product-form.jsp").forward(request, response);
        } catch (NumberFormatException exception) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            request.setAttribute("formNotFound", true);
            request.getRequestDispatcher("/WEB-INF/views/seller-product-form.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller product form request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Product form is temporarily unavailable.");
        }
    }

    private long parseId(String value) {
        long id = Long.parseLong(value);
        if (id <= 0) throw new NumberFormatException();
        return id;
    }
}
