package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.Product;
import com.ashokmart.service.ProductManagementException;
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
import java.math.BigDecimal;

@WebServlet(urlPatterns = {"/seller/products/create", "/seller/products/update", "/seller/products/activate", "/seller/products/deactivate"})
public final class SellerProductMutationServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(SellerProductMutationServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult seller = SellerWebSupport.authenticatedSeller(request, response);
        if (seller == null) return;
        String path = request.getServletPath();
        try {
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            ProductService service = SellerWebSupport.productService(pool);
            if (path.endsWith("/activate") || path.endsWith("/deactivate")) {
                service.updateSellerProductStatus(seller.userId(), parseId(request.getParameter("id")), path.endsWith("/activate"));
                flash(request, "sellerSuccess", path.endsWith("/activate") ? "Product activated." : "Product deactivated.");
            } else if (path.endsWith("/create")) {
                service.createSellerProduct(seller.userId(), productFromRequest(request, 0));
                flash(request, "sellerSuccess", "Product created.");
            } else {
                long productId = parseId(request.getParameter("id"));
                service.updateSellerProduct(seller.userId(), productFromRequest(request, productId));
                flash(request, "sellerSuccess", "Product updated.");
            }
            response.sendRedirect(request.getContextPath() + "/seller/products");
        } catch (ProductManagementException exception) {
            if (path.endsWith("/create") || path.endsWith("/update")) {
                forwardFormError(request, response, path.endsWith("/update"), exception.getMessage());
            } else {
                flash(request, "sellerError", exception.getMessage());
                response.sendRedirect(request.getContextPath() + "/seller/products");
            }
        } catch (NumberFormatException exception) {
            forwardFormError(request, response, path.endsWith("/update"), "Please enter valid numeric values.");
        } catch (IllegalStateException exception) {
            LOGGER.error("Seller product mutation failed", exception);
            if (path.endsWith("/create") || path.endsWith("/update")) {
                forwardFormError(request, response, path.endsWith("/update"), "Product could not be saved. Please try again.");
            } else {
                flash(request, "sellerError", "Product status could not be changed. Please try again.");
                response.sendRedirect(request.getContextPath() + "/seller/products");
            }
        }
    }

    private Product productFromRequest(HttpServletRequest request, long id) {
        long categoryId = Long.parseLong(request.getParameter("categoryId"));
        String priceValue = request.getParameter("price");
        if (priceValue == null || priceValue.isBlank()) throw new ProductManagementException("Price is required");
        BigDecimal price = new BigDecimal(priceValue.trim());
        int stock = Integer.parseInt(request.getParameter("stockQuantity"));
        return new Product(id, 0, categoryId, request.getParameter("name"), request.getParameter("description"),
                price, stock, request.getParameter("imageUrl"), true, null, null);
    }

    private long parseId(String value) {
        long id = Long.parseLong(value);
        if (id <= 0) throw new NumberFormatException();
        return id;
    }

    private void forwardFormError(HttpServletRequest request, HttpServletResponse response,
                                  boolean editing, String message) throws ServletException, IOException {
        request.setAttribute("productError", message);
        request.setAttribute("editing", editing);
        try {
            DatabaseConnectionPool pool = SellerWebSupport.pool(getServletContext());
            request.setAttribute("categories", SellerWebSupport.categoryService(pool).listCategories());
        } catch (IllegalStateException exception) {
            LOGGER.error("Could not reload product categories", exception);
        }
        request.getRequestDispatcher("/WEB-INF/views/seller-product-form.jsp").forward(request, response);
    }

    private void flash(HttpServletRequest request, String key, String value) {
        HttpSession session = request.getSession(true);
        session.setAttribute(key, value);
    }
}
