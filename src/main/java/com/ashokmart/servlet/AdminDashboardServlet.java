package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/admin/dashboard")
public final class AdminDashboardServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDashboardServlet.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        AuthenticationResult admin = AdminWebSupport.authenticatedAdmin(request, response);
        if (admin == null) return;
        try {
            AdminUserService service = AdminWebSupport.adminUserService(AdminWebSupport.pool(getServletContext()));
            request.setAttribute("statistics", service.getStatistics(admin.userId()));
            request.getRequestDispatcher("/WEB-INF/views/admin-dashboard.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOGGER.error("Admin dashboard request failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Admin dashboard is temporarily unavailable.");
        }
    }
}
