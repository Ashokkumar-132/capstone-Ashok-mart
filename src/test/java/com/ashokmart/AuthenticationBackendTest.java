package com.ashokmart;

import com.ashokmart.dao.UserDao;
import com.ashokmart.dao.impl.UserDaoImpl;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.User;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AuthService;
import com.ashokmart.service.AuthenticationException;
import com.ashokmart.service.impl.AuthServiceImpl;
import com.ashokmart.util.DatabaseConfig;
import com.ashokmart.util.DatabaseConnectionPool;
import com.ashokmart.util.DatabaseInitializer;
import com.ashokmart.util.PasswordUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticationBackendTest {
    private DatabaseConnectionPool pool;
    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() throws Exception {
        String databaseName = "auth_" + UUID.randomUUID().toString().replace('-', '_');
        pool = new DatabaseConnectionPool(DatabaseConfig.forTesting("jdbc:h2:mem:" + databaseName));
        DatabaseInitializer.initialize(pool);
        userDao = new UserDaoImpl(pool);
        authService = new AuthServiceImpl(userDao);
    }

    @AfterEach
    void tearDown() {
        pool.close();
    }

    @Test
    void passwordHashesAndVerifiesWithoutStoringPlaintext() {
        String hash = PasswordUtil.hash("correct horse battery staple");
        assertNotEquals("correct horse battery staple", hash);
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(PasswordUtil.verify("correct horse battery staple", hash));
        assertFalse(PasswordUtil.verify("wrong password", hash));
    }

    @Test
    void registrationNormalizesEmailHashesPasswordAndDefaultsToBuyer() throws SQLException {
        AuthenticationResult result = authService.register("  Ashok  ", "  ASHOK@Example.com ", "strong-pass-123");
        assertEquals("Ashok", result.name());
        assertEquals("ashok@example.com", result.email());
        assertEquals(UserRole.BUYER, result.role());

        User stored = userDao.findByEmail("ashok@example.com").orElseThrow();
        assertTrue(PasswordUtil.verify("strong-pass-123", stored.getPasswordHash()));
        assertNotEquals("strong-pass-123", stored.getPasswordHash());
        assertTrue(stored.isEnabled());
    }

    @Test
    void duplicateAndInvalidRegistrationAreRejected() {
        authService.register("Ashok", "duplicate@example.com", "strong-pass-123");
        assertThrows(AuthenticationException.class,
                () -> authService.register("Other", "DUPLICATE@example.com", "strong-pass-123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "valid@example.com", "strong-pass-123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Name", "not-an-email", "strong-pass-123"));
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("Name", "valid@example.com", "short"));
    }

    @Test
    void publicRegistrationCannotSelectAnElevatedRole() throws SQLException {
        AuthenticationResult result = authService.register("Public", "public@example.com", "strong-pass-123");
        assertEquals(UserRole.BUYER, result.role());
        assertEquals(UserRole.BUYER, userDao.findById(result.userId()).orElseThrow().getRole());
    }

    @Test
    void validLoginReturnsSafeResultAndInvalidCredentialsAreGeneric() throws SQLException {
        authService.register("Login User", "login@example.com", "strong-pass-123");
        AuthenticationResult result = authService.login(" LOGIN@EXAMPLE.COM ", "strong-pass-123");
        assertEquals(UserRole.BUYER, result.role());
        assertFalse(result.toString().contains("$2a$"));

        AuthenticationException wrongPassword = assertThrows(AuthenticationException.class,
                () -> authService.login("login@example.com", "wrong-password"));
        AuthenticationException unknownEmail = assertThrows(AuthenticationException.class,
                () -> authService.login("unknown@example.com", "wrong-password"));
        assertEquals("Invalid email or password", wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownEmail.getMessage());
    }

    @Test
    void disabledUsersCannotLoginAndDaoSupportsLookupAndStatusUpdates() throws SQLException {
        AuthenticationResult registered = authService.register("Disabled", "disabled@example.com", "strong-pass-123");
        Optional<User> byId = userDao.findById(registered.userId());
        assertTrue(byId.isPresent());
        assertEquals(UserRole.BUYER, byId.get().getRole());
        userDao.updateEnabled(registered.userId(), false);
        assertFalse(userDao.findById(registered.userId()).orElseThrow().isEnabled());
        assertThrows(AuthenticationException.class,
                () -> authService.login("disabled@example.com", "strong-pass-123"));
    }
}
