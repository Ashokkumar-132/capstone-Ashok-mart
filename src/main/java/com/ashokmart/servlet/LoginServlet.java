package com.ashokmart.servlet;

import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.service.AuthService;
import com.ashokmart.service.AuthenticationException;
import com.ashokmart.service.impl.AuthServiceImpl;
import com.ashokmart.util.DatabaseConnectionPool;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = "/login", loadOnStartup = 0)
public final class LoginServlet extends HttpServlet {
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            AuthService service = authService(request);
            AuthenticationResult result = service.login(
                    request.getParameter("email"),
                    request.getParameter("password"));
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, result);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (IllegalArgumentException | AuthenticationException exception) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid email or password");
        }
    }

    private AuthService authService(HttpServletRequest request) {
        Object pool = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(pool instanceof DatabaseConnectionPool connectionPool)) {
            throw new AuthenticationException("Database is unavailable");
        }
        return new AuthServiceImpl(new UserDaoImpl(connectionPool));
    }
}
