package com.ashokmart.servlet;

import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.AuthService;
import com.ashokmart.service.AuthenticationException;
import com.ashokmart.service.impl.AuthServiceImpl;
import com.ashokmart.util.DatabaseConnectionPool;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/register", loadOnStartup = 0)
public final class RegisterServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        try {
            String password = request.getParameter("password");
            String confirmation = request.getParameter("confirmPassword");
            if (password == null || !password.equals(confirmation)) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            AuthService service = authService(request);
            service.register(request.getParameter("name"), request.getParameter("email"), password);
            response.sendRedirect(request.getContextPath() + "/login?registered=true");
        } catch (IllegalArgumentException | AuthenticationException exception) {
            request.setAttribute("registerError", userMessage(exception));
            request.getRequestDispatcher("/WEB-INF/views/register.jsp").forward(request, response);
        }
    }

    private AuthService authService(HttpServletRequest request) {
        Object pool = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(pool instanceof DatabaseConnectionPool connectionPool)) {
            throw new AuthenticationException("Registration is temporarily unavailable");
        }
        return new AuthServiceImpl(new UserDaoImpl(connectionPool));
    }

    private String userMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "Registration could not be completed" : exception.getMessage();
    }
}
