package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AdminUserService;
import com.ashokmart.service.impl.AdminUserServiceImpl;
import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.util.DatabaseConnectionPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

final class AdminWebSupport {
    private AdminWebSupport() {
    }

    static DatabaseConnectionPool pool(javax.servlet.ServletContext context) {
        return (DatabaseConnectionPool) context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
    }

    static AdminUserService adminUserService(DatabaseConnectionPool pool) {
        return new AdminUserServiceImpl(new UserDaoImpl(pool));
    }

    static AuthenticationResult authenticatedAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        javax.servlet.http.HttpSession session = request.getSession(false);
        Object value = session == null ? null : session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);
        if (!(value instanceof AuthenticationResult admin)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return null;
        }
        if (admin.role() != UserRole.ADMIN) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            request.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(request, response);
            return null;
        }
        return admin;
    }

    static void flash(HttpServletRequest request, String key, String value) {
        request.getSession(true).setAttribute(key, value);
    }
}
