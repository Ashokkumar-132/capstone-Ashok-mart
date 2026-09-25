package com.ashokmart.service.impl;

import com.ashokmart.dao.UserDao;
import com.ashokmart.dao.ProductDao;
import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.AdminStatistics;
import com.ashokmart.model.AdminUserPage;
import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.AdminUserView;
import com.ashokmart.model.User;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AdminUserException;
import com.ashokmart.service.AdminUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class AdminUserServiceImpl implements AdminUserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserServiceImpl.class);
    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;

    public AdminUserServiceImpl(UserDao userDao) {
        this(userDao, null, null);
    }

    public AdminUserServiceImpl(UserDao userDao, ProductDao productDao, OrderDao orderDao) {
        this.userDao = userDao;
        this.productDao = productDao;
        this.orderDao = orderDao;
    }

    @Override
    public AdminStatistics getStatistics(long authenticatedAdminId) {
        requireAdmin(authenticatedAdminId);
        try {
            AdminStatistics users = new AdminStatistics(userDao.countUsers(), userDao.countUsersByRole(UserRole.BUYER),
                    userDao.countUsersByRole(UserRole.SELLER), userDao.countUsersByRole(UserRole.ADMIN),
                    userDao.countUsersByStatus(true), userDao.countUsersByStatus(false));
            if (productDao == null || orderDao == null) return users;
            return new AdminStatistics(users.getTotalUsers(), users.getBuyers(), users.getSellers(), users.getAdmins(), users.getActiveUsers(), users.getInactiveUsers(),
                    productDao.countByStatus(null), productDao.countByStatus(true), productDao.countByStatus(false), productDao.countByStock(true), productDao.countByStock(false),
                    orderDao.countOrders(), orderDao.totalRevenue(), orderDao.averageOrderValue());
        } catch (SQLException exception) {
            LOGGER.error("Admin statistics lookup failed", exception);
            throw new IllegalStateException("Admin statistics are temporarily unavailable", exception);
        }
    }

    @Override
    public AdminUserPage getUsers(long authenticatedAdminId, AdminUserQuery query) {
        requireAdmin(authenticatedAdminId);
        if (query == null) throw new AdminUserException("A valid user query is required");
        try {
            long total = userDao.countUsers(query);
            int totalPages = Math.max(1, (int) Math.ceil((double) total / query.getPageSize()));
            int safePage = Math.min(query.getPage(), totalPages);
            AdminUserQuery safeQuery = new AdminUserQuery(query.getSearch(), query.getRole(), query.getEnabled(), safePage, query.getPageSize());
            List<AdminUserView> users = userDao.findUsers(safeQuery).stream().map(this::safeView).toList();
            return new AdminUserPage(users, safePage, safeQuery.getPageSize(), total, totalPages);
        } catch (SQLException exception) {
            LOGGER.error("Admin user listing failed", exception);
            throw new IllegalStateException("Users are temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<AdminUserView> getUser(long authenticatedAdminId, long userId) {
        requireAdmin(authenticatedAdminId);
        if (userId <= 0) throw new AdminUserException("A valid user is required");
        try {
            return userDao.findById(userId).map(this::safeView);
        } catch (SQLException exception) {
            LOGGER.error("Admin user lookup failed", exception);
            throw new IllegalStateException("User details are temporarily unavailable", exception);
        }
    }

    @Override
    public void activateUser(long authenticatedAdminId, long userId) {
        changeStatus(authenticatedAdminId, userId, true);
    }

    @Override
    public void deactivateUser(long authenticatedAdminId, long userId) {
        changeStatus(authenticatedAdminId, userId, false);
    }

    private void changeStatus(long authenticatedAdminId, long userId, boolean enabled) {
        requireAdmin(authenticatedAdminId);
        if (userId <= 0) throw new AdminUserException("A valid user is required");
        try {
            User target = userDao.findById(userId).orElseThrow(() -> new AdminUserException("User not found"));
            if (target.isEnabled() == enabled) {
                throw new AdminUserException("User account is already " + (enabled ? "active" : "inactive"));
            }
            if (!enabled && target.getId() == authenticatedAdminId) {
                throw new AdminUserException("You cannot deactivate the currently authenticated admin account");
            }
            if (!enabled && target.getRole() == UserRole.ADMIN && target.isEnabled()
                    && userDao.countUsersByRoleAndStatus(UserRole.ADMIN, true) <= 1) {
                throw new AdminUserException("The final active admin account cannot be deactivated");
            }
            if (!userDao.updateUserStatus(userId, enabled)) throw new AdminUserException("User not found");
        } catch (AdminUserException exception) {
            throw exception;
        } catch (SQLException exception) {
            LOGGER.error("Admin user status update failed", exception);
            throw new IllegalStateException("User status could not be updated", exception);
        }
    }

    private void requireAdmin(long authenticatedAdminId) {
        if (authenticatedAdminId <= 0) throw new AdminUserException("You must be logged in as an admin");
        try {
            User admin = userDao.findById(authenticatedAdminId).orElse(null);
            if (admin == null || !admin.isEnabled() || admin.getRole() != UserRole.ADMIN) {
                throw new AdminUserException("Admin access is required");
            }
        } catch (SQLException exception) {
            LOGGER.error("Admin authorization lookup failed", exception);
            throw new IllegalStateException("Admin access is temporarily unavailable", exception);
        }
    }

    private AdminUserView safeView(User user) {
        return new AdminUserView(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
