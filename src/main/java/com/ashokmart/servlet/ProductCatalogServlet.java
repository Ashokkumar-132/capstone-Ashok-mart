package com.ashokmart.servlet;

import com.ashokmart.dao.impl.CategoryDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.ProductSearchCriteria;
import com.ashokmart.model.ProductSort;
import com.ashokmart.service.CatalogValidationException;
import com.ashokmart.service.CategoryService;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.impl.CategoryServiceImpl;
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
import java.math.BigDecimal;

@WebServlet(urlPatterns = "/products")
public final class ProductCatalogServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCatalogServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        StringBuilder validationMessage = new StringBuilder();
        String searchTerm = trimmed(request.getParameter("q"));
        Long categoryId = parsePositiveLong(request.getParameter("category"), "category", validationMessage);
        BigDecimal minimumPrice = parsePrice(request.getParameter("minPrice"), "minimum price", validationMessage);
        BigDecimal maximumPrice = parsePrice(request.getParameter("maxPrice"), "maximum price", validationMessage);
        boolean inStockOnly = "in_stock".equalsIgnoreCase(request.getParameter("stock"));
        int page = parsePositiveInt(request.getParameter("page"), ProductServiceImpl.DEFAULT_PAGE, "page", validationMessage);
        int pageSize = parsePositiveInt(request.getParameter("size"), ProductServiceImpl.DEFAULT_PAGE_SIZE, "page size", validationMessage);
        ProductSort sort = ProductSort.fromExternal(request.getParameter("sort"));

        try {
            CategoryService categoryService = categoryService(request);
            request.setAttribute("categories", categoryService.listCategories());
            request.setAttribute("searchTerm", searchTerm);
            request.setAttribute("selectedCategoryId", categoryId);
            request.setAttribute("minimumPrice", minimumPrice);
            request.setAttribute("maximumPrice", maximumPrice);
            request.setAttribute("inStockOnly", inStockOnly);
            request.setAttribute("selectedSort", sortKey(sort));
            if (validationMessage.length() > 0) {
                request.setAttribute("catalogError", validationMessage.toString());
            }
            ProductService productService = productService(request);
            request.setAttribute("productPage", productService.search(new ProductSearchCriteria(searchTerm, categoryId,
                    minimumPrice, maximumPrice, inStockOnly, sort, page, pageSize)));
            request.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(request, response);
        } catch (CatalogValidationException exception) {
            request.setAttribute("catalogError", exception.getMessage());
            request.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Catalog listing failed", exception);
            request.setAttribute("catalogError", "The catalog is temporarily unavailable. Please try again.");
            request.getRequestDispatcher("/WEB-INF/views/products.jsp").forward(request, response);
        }
    }

    private ProductService productService(HttpServletRequest request) {
        DatabaseConnectionPool pool = pool(request);
        return new ProductServiceImpl(new ProductDaoImpl(pool));
    }

    private CategoryService categoryService(HttpServletRequest request) {
        DatabaseConnectionPool pool = pool(request);
        return new CategoryServiceImpl(new CategoryDaoImpl(pool));
    }

    private DatabaseConnectionPool pool(HttpServletRequest request) {
        Object value = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Catalog database is unavailable");
        }
        return pool;
    }

    private String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long parsePositiveLong(String value, String label, StringBuilder message) {
        if (value == null || value.isBlank()) return null;
        try {
            long parsed = Long.parseLong(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            appendMessage(message, "The " + label + " filter was ignored.");
            return null;
        }
    }

    private BigDecimal parsePrice(String value, String label, StringBuilder message) {
        if (value == null || value.isBlank()) return null;
        try {
            BigDecimal parsed = new BigDecimal(value);
            if (parsed.signum() < 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            appendMessage(message, "The " + label + " filter was ignored.");
            return null;
        }
    }

    private int parsePositiveInt(String value, int fallback, String label, StringBuilder message) {
        if (value == null || value.isBlank()) return fallback;
        try {
            int parsed = Integer.parseInt(value);
            if (parsed <= 0) throw new NumberFormatException();
            return parsed;
        } catch (NumberFormatException exception) {
            appendMessage(message, "The " + label + " value was reset to a safe default.");
            return fallback;
        }
    }

    private void appendMessage(StringBuilder message, String addition) {
        if (message.length() > 0) message.append(' ');
        message.append(addition);
    }

    private String sortKey(ProductSort sort) {
        return switch (sort) {
            case PRICE_ASC -> "price_asc";
            case PRICE_DESC -> "price_desc";
            case NAME_ASC -> "name_asc";
            case NAME_DESC -> "name_desc";
            case NEWEST -> "newest";
        };
    }
}
