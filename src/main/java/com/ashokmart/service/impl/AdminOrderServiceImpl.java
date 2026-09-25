package com.ashokmart.service.impl;
import com.ashokmart.dao.OrderDao;
import com.ashokmart.dao.UserDao;
import com.ashokmart.model.*;
import com.ashokmart.service.AdminOrderService;
import com.ashokmart.service.AdminUserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.SQLException;
import java.util.Optional;
public final class AdminOrderServiceImpl implements AdminOrderService {
    private static final Logger LOGGER=LoggerFactory.getLogger(AdminOrderServiceImpl.class); private final OrderDao orders; private final UserDao users;
    public AdminOrderServiceImpl(OrderDao orders,UserDao users){this.orders=orders;this.users=users;}
    public AdminOrderPage getOrders(long adminId,AdminOrderModels.Query query){requireAdmin(adminId);if(query==null)throw new AdminUserException("A valid order query is required");try{long total=orders.countAdminOrders(query);int pages=Math.max(1,(int)Math.ceil((double)total/query.pageSize()));int page=Math.min(query.page(),pages);AdminOrderModels.Query safe=new AdminOrderModels.Query(query.search(),query.status(),page,query.pageSize());return new AdminOrderPage(orders.findAdminOrders(safe),page,safe.pageSize(),total,pages);}catch(SQLException e){LOGGER.error("Admin order listing failed",e);throw new IllegalStateException("Orders are temporarily unavailable",e);}}
    public Optional<AdminOrderModels.Details> getOrder(long adminId,long orderId){requireAdmin(adminId);if(orderId<=0)throw new AdminUserException("A valid order is required");try{return orders.findAdminOrderDetails(orderId);}catch(SQLException e){LOGGER.error("Admin order lookup failed",e);throw new IllegalStateException("Order details are temporarily unavailable",e);}}
    private void requireAdmin(long id){if(id<=0)throw new AdminUserException("Admin access is required");try{User u=users.findById(id).orElse(null);if(u==null||!u.isEnabled()||u.getRole()!=UserRole.ADMIN)throw new AdminUserException("Admin access is required");}catch(SQLException e){LOGGER.error("Admin order authorization failed",e);throw new IllegalStateException("Admin access is temporarily unavailable",e);}}
}
