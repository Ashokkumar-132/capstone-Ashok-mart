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
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            AuthService service = authService(request);
            AuthenticationResult result = service.register(
                    request.getParameter("name"),
                    request.getParameter("email"),
                    request.getParameter("password"));
            response.setStatus(HttpServletResponse.SC_CREATED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Registration successful for " + result.email());
        } catch (IllegalArgumentException | AuthenticationException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    private AuthService authService(HttpServletRequest request) {
        DatabaseConnectionPool pool = pool(request);
        return new AuthServiceImpl(new UserDaoImpl(pool));
    }

    private DatabaseConnectionPool pool(HttpServletRequest request) {
        Object pool = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(pool instanceof DatabaseConnectionPool connectionPool)) {
            throw new AuthenticationException("Database is unavailable");
        }
        return connectionPool;
    }
}
