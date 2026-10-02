package com.ashokmart.servlet;

import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.ReviewService;
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
            DatabaseConnectionPool reviewPool = pool(request);
            ReviewService reviewService = ReviewWebSupport.service(reviewPool);
            request.setAttribute("reviews", reviewService.getProductReviews(productId));
            request.setAttribute("reviewCount", reviewService.getReviewCount(productId));
            request.setAttribute("averageRating", reviewService.getAverageRating(productId));
            javax.servlet.http.HttpSession session = request.getSession(false);
            Object auth = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
            if (auth instanceof com.ashokmart.model.AuthenticationResult buyer && buyer.role() == com.ashokmart.model.UserRole.BUYER) {
                request.setAttribute("buyerReview", reviewService.getBuyerReview(buyer.userId(), productId).orElse(null));
                request.setAttribute("canReview", reviewService.canReview(buyer.userId(), productId));
            }
            moveFlash(session, request, "reviewSuccess", "reviewError");
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

    private DatabaseConnectionPool pool(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) throw new IllegalStateException("Review database is unavailable");
        return pool;
    }

    private void moveFlash(javax.servlet.http.HttpSession session, HttpServletRequest request, String... keys) {
        if (session == null) return;
        for (String key : keys) { Object value = session.getAttribute(key); if (value != null) { request.setAttribute(key, value); session.removeAttribute(key); } }
    }

    private void forwardNotFound(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        request.setAttribute("productNotFound", true);
        request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp").forward(request, response);
    }
}
