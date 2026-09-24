package com.ashokmart.servlet;

import com.ashokmart.dao.impl.CategoryDaoImpl;
import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.dao.impl.OrderDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.CategoryService;
import com.ashokmart.service.ProductService;
import com.ashokmart.service.SellerOrderService;
import com.ashokmart.service.impl.CategoryServiceImpl;
import com.ashokmart.service.impl.ProductServiceImpl;
import com.ashokmart.service.impl.SellerOrderServiceImpl;
import com.ashokmart.util.DatabaseConnectionPool;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

final class SellerWebSupport {
    private SellerWebSupport() {
    }

    static AuthenticationResult authenticatedSeller(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(value instanceof AuthenticationResult seller)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (seller.role() != UserRole.SELLER) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return seller;
    }

    static DatabaseConnectionPool pool(ServletContext context) {
        Object value = context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(value instanceof DatabaseConnectionPool pool)) {
            throw new IllegalStateException("Seller database is unavailable");
        }
        return pool;
    }

    static ProductService productService(DatabaseConnectionPool pool) {
        CategoryService categories = new CategoryServiceImpl(new CategoryDaoImpl(pool));
        return new ProductServiceImpl(new ProductDaoImpl(pool), categories);
    }

    static CategoryService categoryService(DatabaseConnectionPool pool) {
        return new CategoryServiceImpl(new CategoryDaoImpl(pool));
    }

    static SellerOrderService sellerOrderService(DatabaseConnectionPool pool) {
        return new SellerOrderServiceImpl(new OrderDaoImpl(pool));
    }
}
