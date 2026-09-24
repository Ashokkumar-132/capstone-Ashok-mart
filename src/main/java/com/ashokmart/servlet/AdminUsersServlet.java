package com.ashokmart.servlet;

import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = "/admin/users")
public final class AdminUsersServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUsersServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult admin = AdminWebSupport.authenticatedAdmin(request, response);
        if (admin == null) return;
        String search = request.getParameter("q");
        if (search == null) search = "";
        if (search.length() > 100) search = search.substring(0, 100);
        UserRole role = parseRole(request.getParameter("role"));
        Boolean enabled = parseEnabled(request.getParameter("status"));
        int page = parsePositive(request.getParameter("page"), 1);
        int pageSize = parsePositive(request.getParameter("size"), 10);
        AdminUserQuery query = new AdminUserQuery(search, role, enabled, page, pageSize);
        try {
            AdminUserService service = AdminWebSupport.adminUserService(AdminWebSupport.pool(getServletContext()));
            request.setAttribute("userPage", service.getUsers(admin.userId(), query));
            request.setAttribute("search", search);
            request.setAttribute("roleFilter", request.getParameter("role"));
            request.setAttribute("statusFilter", request.getParameter("status"));
            HttpSession session = request.getSession(false);
            if (session != null) {
                moveFlash(session, request, "adminUserSuccess");
                moveFlash(session, request, "adminUserError");
            }
            request.getRequestDispatcher("/WEB-INF/views/admin-users.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Admin user list request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Users are temporarily unavailable.");
        }
    }

    private UserRole parseRole(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        try { return UserRole.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException exception) { return null; }
    }

    private Boolean parseEnabled(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) return null;
        if ("ACTIVE".equalsIgnoreCase(value)) return true;
        if ("INACTIVE".equalsIgnoreCase(value)) return false;
        return null;
    }

    private int parsePositive(String value, int fallback) {
        try { return value == null ? fallback : Math.max(1, Integer.parseInt(value)); }
        catch (NumberFormatException exception) { return fallback; }
    }

    private void moveFlash(HttpSession session, HttpServletRequest request, String key) {
        Object value = session.getAttribute(key);
        if (value != null) { request.setAttribute(key, value); session.removeAttribute(key); }
    }
}
