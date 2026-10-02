package com.ashokmart.servlet;

import com.ashokmart.model.AdminOrderModels;
import com.ashokmart.service.AdminOrderService;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@WebServlet("/admin/orders")
public final class AdminOrdersServlet extends HttpServlet {
    private static final Logger LOG = LoggerFactory.getLogger(AdminOrdersServlet.class);
    private static final Set<String> STATUSES = Set.of("PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        var admin = AdminWebSupport.authenticatedAdmin(request, response);
        if (admin == null) return;
        try {
            String status = request.getParameter("status");
            if (status != null) {
                status = status.trim().toUpperCase(Locale.ROOT);
                if (!STATUSES.contains(status)) status = null;
            }
            int page;
            try { page = ValidationUtil.page(request.getParameter("page")); }
            catch (IllegalArgumentException ignored) { page = ValidationUtil.DEFAULT_PAGE; }
            String search = request.getParameter("search");
            if (search != null && search.length() > 100) search = search.substring(0, 100);
            AdminOrderService service = AdminWebSupport.adminOrderService(AdminWebSupport.pool(getServletContext()));
            request.setAttribute("orderPage", service.getOrders(admin.userId(), new AdminOrderModels.Query(search, status, page, 12)));
            request.setAttribute("selectedStatus", status);
            request.setAttribute("searchTerm", search);
            AdminWebSupport.moveFlash(request.getSession(false), request, "adminSuccess", "adminError");
            request.getRequestDispatcher("/WEB-INF/views/admin-orders.jsp").forward(request, response);
        } catch (IllegalStateException exception) {
            LOG.error("Admin order listing failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Orders are temporarily unavailable.");
        }
    }
}
