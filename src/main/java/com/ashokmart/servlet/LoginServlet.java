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
import javax.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(urlPatterns = "/login", loadOnStartup = 0)
public final class LoginServlet extends HttpServlet {
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        request.setCharacterEncoding("UTF-8");
        try {
            AuthService service = authService(request);
            AuthenticationResult result = service.login(request.getParameter("email"), request.getParameter("password"));
            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, result);
            response.sendRedirect(request.getContextPath() + "/");
        } catch (IllegalArgumentException | AuthenticationException exception) {
            request.setAttribute("loginError", "Invalid email or password");
            request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
        }
    }

    private AuthService authService(HttpServletRequest request) {
        Object pool = getServletContext().getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);
        if (!(pool instanceof DatabaseConnectionPool connectionPool)) {
            throw new AuthenticationException("Authentication is temporarily unavailable");
        }
        return new AuthServiceImpl(new UserDaoImpl(connectionPool));
    }
}
