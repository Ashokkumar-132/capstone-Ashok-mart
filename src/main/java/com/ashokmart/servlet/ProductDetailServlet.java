package com.ashokmart.servlet;

import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.impl.ProductServiceImpl;
import com.ashokmart.util.DatabaseConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/product")
public final class ProductDetailServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDetailServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        long productId;
        try {
            productId = Long.parseLong(request.getParameter("id"));
            if (productId <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            forwardNotFound(request, response);
            return;
        }

        try {
            ProductService service = service(request);
            var product = service.findProduct(productId);
            if (product.isEmpty()) {
                forwardNotFound(request, response);
                return;
            }
            request.setAttribute("product", product.get());
            request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Product detail lookup failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The product is temporarily unavailable.");
        }
    }

    private ProductService service(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Catalog database is unavailable");
        }
        return new ProductServiceImpl(new ProductDaoImpl(pool));
    }

    private void forwardNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("productNotFound", true);
        request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(request, response);
    }
}
