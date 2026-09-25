package com.ashokmart.service.impl;
import com.ashokmart.dao.ProductDao;
import com.ashokmart.dao.UserDao;
import com.ashokmart.model.*;
import com.ashokmart.service.AdminProductService;
import com.ashokmart.service.AdminUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Optional;
public final class AdminProductServiceImpl implements AdminProductService {
    private static final Logger LOGGER=LoggerFactory.getLogger(AdminProductServiceImpl.class);
    private final ProductDao products; private final UserDao users;
    public AdminProductServiceImpl(ProductDao products, UserDao users){this.products=products;this.users=users;}
    public AdminProductPage getProducts(long adminId, AdminProductQuery query){requireAdmin(adminId);if(query==null)throw new AdminUserException("A valid product query is required");try{long total=products.countAdminProducts(query);int pages=Math.max(1,(int)Math.ceil((double)total/query.pageSize()));int page=Math.min(query.page(),pages);AdminProductQuery safe=new AdminProductQuery(query.search(),query.categoryId(),query.active(),query.inStock(),page,query.pageSize());return new AdminProductPage(products.findAdminProducts(safe),page,safe.pageSize(),total,pages);}catch(SQLException e){LOGGER.error("Admin product listing failed",e);throw new IllegalStateException("Products are temporarily unavailable",e);}}
    public Optional<ProductSummary> getProduct(long adminId,long productId){requireAdmin(adminId);if(productId<=0)throw new AdminUserException("A valid product is required");try{return products.findAdminSummaryById(productId);}catch(SQLException e){LOGGER.error("Admin product lookup failed",e);throw new IllegalStateException("Product details are temporarily unavailable",e);}}
    public void setStatus(long adminId,long productId,boolean active){requireAdmin(adminId);if(productId<=0)throw new AdminUserException("A valid product is required");try{if(products.findById(productId).isEmpty())throw new AdminUserException("Product not found");if(!products.updateAdminStatus(productId,active))throw new AdminUserException("Product not found");}catch(AdminUserException e){throw e;}catch(SQLException e){LOGGER.error("Admin product status update failed",e);throw new IllegalStateException("Product status could not be changed",e);}}
    private void requireAdmin(long id){if(id<=0)throw new AdminUserException("Admin access is required");try{User u=users.findById(id).orElse(null);if(u==null||!u.isEnabled()||u.getRole()!=UserRole.ADMIN)throw new AdminUserException("Admin access is required");}catch(SQLException e){LOGGER.error("Admin product authorization failed",e);throw new IllegalStateException("Admin access is temporarily unavailable",e);}}
}
