package com.ashokmart;

import com.ashokmart.model.ProductPage;
import com.ashokmart.servlet.ProductCatalogServlet;
import com.ashokmart.servlet.ProductDetailServlet;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductCatalogServletTest {
    private DatabaseConnectionPool pool;
    private ServletContext context;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "catalog_servlet_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        try (Connection connection = pool.getConnection()) {
            execute(connection, "INSERT INTO users (id, name, email, password_hash, role) VALUES (1, 'Seller', 'seller@servlet.test', 'hash', 'SELLER')");
            execute(connection, "INSERT INTO categories (id, name) VALUES (1, 'Electronics')");
            execute(connection, "INSERT INTO products (id, seller_id, category_id, name, price, stock_quantity) VALUES (1, 1, 1, 'Phone', 12.50, 3)");
        }
        context = mock(ServletContext.class);
        when(context.getAttribute(DatabaseConnectionPool.CONTEXT_ATTRIBUTE)).thenReturn(pool);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void listingServletUsesQueryParametersAndForwardsCatalogPage() throws Exception {
        ProductCatalogServlet servlet = spy(new ProductCatalogServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/products.jsp")).thenReturn(dispatcher);
        when(request.getParameter("q")).thenReturn("phone");
        when(request.getParameter("category")).thenReturn("1");
        when(request.getParameter("minPrice")).thenReturn("10");
        when(request.getParameter("maxPrice")).thenReturn("20");
        when(request.getParameter("stock")).thenReturn("in_stock");
        when(request.getParameter("sort")).thenReturn("price_asc");
        when(request.getParameter("page")).thenReturn("1");
        when(request.getParameter("size")).thenReturn("10");

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("productPage"), any(ProductPage.class));
        verify(request).setAttribute("selectedSort", "price_asc");
        verify(dispatcher).forward(request, response);
    }

    @Test
    void invalidListingParametersAreHandledWithoutExposingDatabaseErrors() throws Exception {
        ProductCatalogServlet servlet = spy(new ProductCatalogServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/products.jsp")).thenReturn(dispatcher);
        when(request.getParameter("category")).thenReturn("not-a-number");
        when(request.getParameter("minPrice")).thenReturn("-5");
        when(request.getParameter("page")).thenReturn("0");
        when(request.getParameter("size")).thenReturn("-1");

        servlet.doGet(request, response);

        verify(request).setAttribute(eq("catalogError"), any(String.class));
        verify(dispatcher).forward(request, response);
    }

    @Test
    void detailServletForwardsActiveProductAndReturnsCleanNotFoundState() throws Exception {
        ProductDetailServlet servlet = spy(new ProductDetailServlet());
        doReturn(context).when(servlet).getServletContext();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);
        when(request.getRequestDispatcher("/WEB-INF/views/product-detail.jsp")).thenReturn(dispatcher);
        when(request.getParameter("id")).thenReturn("1");

        servlet.doGet(request, response);
        verify(request).setAttribute(eq("product"), any(Object.class));
        verify(dispatcher).forward(request, response);

        when(request.getParameter("id")).thenReturn("999");
        servlet.doGet(request, response);
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
        verify(request).setAttribute("productNotFound", true);
    }

    private void execute(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        }
    }
}
