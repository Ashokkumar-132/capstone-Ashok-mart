package com.ashokmart;

import com.ashokmart.dao.UserDao;
import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.model.AdminUserPage;
import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.AdminUserView;
import com.ashokmart.model.AdminStatistics;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AdminUserException;
import com.ashokmart.service.AdminUserService;
import com.ashokmart.service.impl.AdminUserServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminUserBackendTest {
    private DatabaseConnectionPool pool;
    private UserDao userDao;
    private AdminUserService service;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "admin_users_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        insertFixtures();
        userDao = new UserDaoImpl(pool);
        service = new AdminUserServiceImpl(userDao);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void statisticsAndFiltersUseRealUsersWithoutExposingHashes() throws Exception {
        AdminStatistics statistics = service.getStatistics(1);
        assertEquals(5, statistics.getTotalUsers());
        assertEquals(1, statistics.getBuyers());
        assertEquals(2, statistics.getSellers());
        assertEquals(2, statistics.getAdmins());
        assertEquals(4, statistics.getActiveUsers());
        assertEquals(1, statistics.getInactiveUsers());

        AdminUserPage sellers = service.getUsers(1, new AdminUserQuery("seller", UserRole.SELLER, true, 1, 10));
        assertEquals(1, sellers.getTotalUsers());
        assertEquals(1, sellers.getUsers().size());
        assertTrue(sellers.getUsers().stream().allMatch(user -> user.getRole() == UserRole.SELLER));
        assertThrows(NoSuchMethodException.class, () -> AdminUserView.class.getMethod("getPasswordHash"));
    }

    @Test
    void paginationAndStatusFilteringAreBounded() {
        AdminUserPage inactive = service.getUsers(1, new AdminUserQuery("", null, false, 99, 1));
        assertEquals(1, inactive.getTotalUsers());
        assertEquals(1, inactive.getPage());
        assertEquals(1, inactive.getUsers().size());
        assertFalse(inactive.getUsers().get(0).isEnabled());
    }

    @Test
    void adminCanManageBuyerButCannotRemoveAdminAccess() throws Exception {
        service.deactivateUser(1, 4);
        assertFalse(userDao.findById(4).orElseThrow().isEnabled());
        service.activateUser(1, 4);
        assertTrue(userDao.findById(4).orElseThrow().isEnabled());
        assertThrows(AdminUserException.class, () -> service.deactivateUser(1, 1));
        service.deactivateUser(1, 2);
        assertThrows(AdminUserException.class, () -> service.deactivateUser(1, 1));
        assertThrows(AdminUserException.class, () -> service.deactivateUser(1, 2));
    }

    @Test
    void nonAdminAndInvalidTargetsAreRejected() {
        assertThrows(AdminUserException.class, () -> service.getStatistics(3));
        assertThrows(AdminUserException.class, () -> service.getUser(1, 0));
        assertThrows(AdminUserException.class, () -> service.deactivateUser(1, 999));
    }

    private void insertFixtures() throws Exception {
        try (Connection connection = pool.getConnection()) {
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (1, 'Primary Admin', 'admin1@fixture.test', 'hash', 'ADMIN', TRUE)");
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (2, 'Backup Admin', 'admin2@fixture.test', 'hash', 'ADMIN', TRUE)");
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (3, 'Buyer', 'buyer@fixture.test', 'hash', 'BUYER', TRUE)");
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (4, 'Seller One', 'seller1@fixture.test', 'hash', 'SELLER', TRUE)");
            insert(connection, "INSERT INTO users (id, name, email, password_hash, role, enabled) VALUES (5, 'Seller Two', 'seller2@fixture.test', 'hash', 'SELLER', FALSE)");
        }
    }

    private void insert(Connection connection, String sql) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { statement.executeUpdate(); }
    }
}
