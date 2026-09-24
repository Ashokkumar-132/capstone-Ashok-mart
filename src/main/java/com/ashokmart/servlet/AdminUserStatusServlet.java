package com.ashokmart.servlet;

import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.AdminUserException;
import com.ashokmart.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = {"/admin/users/activate", "/admin/users/deactivate"})
public final class AdminUserStatusServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserStatusServlet.class);

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        AuthenticationResult admin = AdminWebSupport.authenticatedAdmin(request, response);
        if (admin == null) return;
        try {
            long userId = Long.parseLong(request.getParameter("userId"));
            if (userId <= 0) throw new NumberFormatException();
            boolean activate = request.getServletPath().endsWith("/activate");
            AdminUserService service = AdminWebSupport.adminUserService(AdminWebSupport.pool(getServletContext()));
            if (activate) {
                service.activateUser(admin.userId(), userId);
                AdminWebSupport.flash(request, "adminUserSuccess", "User account activated.");
            } else {
                service.deactivateUser(admin.userId(), userId);
                AdminWebSupport.flash(request, "adminUserSuccess", "User account deactivated.");
            }
        } catch (NumberFormatException exception) {
            AdminWebSupport.flash(request, "adminUserError", "Please provide a valid user.");
        } catch (AdminUserException exception) {
            AdminWebSupport.flash(request, "adminUserError", exception.getMessage());
        } catch (IllegalStateException exception) {
            LOGGER.error("Admin user status request failed", exception);
            AdminWebSupport.flash(request, "adminUserError", "User status could not be changed. Please try again.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/users");
    }
}
