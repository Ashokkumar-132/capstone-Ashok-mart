package com.ashokmart.servlet;
import com.ashokmart.dao.impl.*; import com.ashokmart.model.*; import com.ashokmart.service.ReviewService; import com.ashokmart.service.impl.ReviewServiceImpl; import com.ashokmart.util.DatabaseConnectionPool; import javax.servlet.*; import javax.servlet.http.*; import java.io.*;
final class ReviewWebSupport { private ReviewWebSupport(){}
 static ReviewService service(DatabaseConnectionPool pool){return new ReviewServiceImpl(new ReviewDaoImpl(pool),new ProductDaoImpl(pool));}
 static AuthenticationResult authenticatedBuyer(HttpServletRequest req,HttpServletResponse res)throws IOException,ServletException{HttpSession session=req.getSession(false);Object value=session==null?null:session.getAttribute(LoginServlet.AUTHENTICATED_USER_ATTRIBUTE);if(!(value instanceof AuthenticationResult buyer)){res.sendRedirect(req.getContextPath()+"/login");return null;}if(buyer.role()!=UserRole.BUYER){res.setStatus(HttpServletResponse.SC_FORBIDDEN);req.getRequestDispatcher("/WEB-INF/views/error/403.jsp").forward(req,res);return null;}return buyer;}
 static DatabaseConnectionPool pool(ServletContext context){Object value=context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE);if(!(value instanceof DatabaseConnectionPool p))throw new IllegalStateException("Review database unavailable");return p;}
 static void flash(HttpServletRequest req,String key,String value){req.getSession(true).setAttribute(key,value);}
}
