package com.ashokmart;

import com.ashokmart.dao.impl.ProductDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.UserRole;
import com.ashokmart.servlet.SellerProductMutationServlet;
import com.ashokmart.servlet.SellerProductsServlet;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SellerServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;
    private HttpSession sellerSession;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "seller_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        TestDatabaseFixtures.insertSellerCatalog(pool);
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
        sellerSession = mock(HttpSession.class);
        when(sellerSession.getAttribute("authenticatedUser"))
                .thenReturn(new AuthenticationResult(3, "Seller One", "seller1@fixture.test", UserRole.SELLER));
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void unauthenticatedSellerProductsRedirectToLogin() throws Exception {
        SellerProductsServlet servlet = new SellerProductsServlet();
        HttpServletRequest request = request("/app/seller/products", null);
        HttpServletResponse response = mock(HttpServletResponse.class);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/app/login");
    }

    @Test
    void sellerCanCreateWithoutClientSellerIdAndSeesOnlyOwnList() throws Exception {
        SellerProductMutationServlet mutation = spy(new SellerProductMutationServlet());
        doReturn(context).when(mutation).getServletContext();
        HttpServletRequest create = request("/app/seller/products/create", sellerSession);
        when(create.getParameter("sellerId")).thenReturn("4");
        when(create.getParameter("categoryId")).thenReturn("1");
        when(create.getParameter("name")).thenReturn("Created by seller one");
        when(create.getParameter("description")).thenReturn("Description");
        when(create.getParameter("price")).thenReturn("19.95");
        when(create.getParameter("stockQuantity")).thenReturn("6");
        when(create.getParameter("imageUrl")).thenReturn("/images/created.jpg");
        HttpServletResponse response = mock(HttpServletResponse.class);

        mutation.doPost(create, response);

        verify(response).sendRedirect("/app/seller/products");
        var own = new ProductDaoImpl(pool).findAllBySellerId(3);
        org.junit.jupiter.api.Assertions.assertEquals(1, own.size());
        org.junit.jupiter.api.Assertions.assertEquals("Created by seller one", own.get(0).getName());
        org.junit.jupiter.api.Assertions.assertTrue(new ProductDaoImpl(pool).findByIdAndSellerId(2, 3).isEmpty());
    }

    @Test
    void changingAnotherSellersProductIdCannotUpdateIt() throws Exception {
        SellerProductMutationServlet mutation = spy(new SellerProductMutationServlet());
        doReturn(context).when(mutation).getServletContext();
        HttpServletRequest update = request("/app/seller/products/update", sellerSession);
        when(update.getParameter("id")).thenReturn("2");
        when(update.getParameter("categoryId")).thenReturn("1");
        when(update.getParameter("name")).thenReturn("Attacked");
        when(update.getParameter("description")).thenReturn("No");
        when(update.getParameter("price")).thenReturn("1.00");
        when(update.getParameter("stockQuantity")).thenReturn("1");
        when(update.getParameter("imageUrl")).thenReturn(null);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(update.getRequestDispatcher("/WEB-INF/views/seller-product-form.jsp")).thenReturn(dispatcher);

        mutation.doPost(update, response);

        verify(update).setAttribute(eq("productError"), any());
        verify(dispatcher).forward(update, response);
        org.junit.jupiter.api.Assertions.assertEquals("Other seller product", new ProductDaoImpl(pool).findByIdAndSellerId(2, 4).orElseThrow().getName());
    }

    @Test
    void changingAnotherSellersStatusCannotDeactivateIt() throws Exception {
        SellerProductMutationServlet mutation = spy(new SellerProductMutationServlet());
        doReturn(context).when(mutation).getServletContext();
        HttpServletRequest deactivate = request("/app/seller/products/deactivate", sellerSession);
        when(deactivate.getParameter("id")).thenReturn("2");
        HttpServletResponse response = mock(HttpServletResponse.class);

        mutation.doPost(deactivate, response);

        verify(response).sendRedirect("/app/seller/products");
        org.junit.jupiter.api.Assertions.assertTrue(new ProductDaoImpl(pool).findByIdAndSellerId(2, 4).orElseThrow().isActive());
    }

    private HttpServletRequest request(String uri, HttpSession session) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("/app");
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getServletPath()).thenReturn(uri.substring("/app".length()));
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession(true)).thenReturn(session);
        return request;
    }
}
