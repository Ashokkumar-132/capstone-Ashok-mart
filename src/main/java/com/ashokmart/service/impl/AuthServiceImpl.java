package com.ashokmart.service.impl;

import com.ashokmart.dao.UserDao;
import com.ashokmart.model.AuthenticationResult;
import com.ashokmart.model.User;
import com.ashokmart.model.UserRole;
import com.ashokmart.service.AuthService;
import com.ashokmart.service.AuthenticationException;
import com.ashokmart.util.AuthValidation;
import com.ashokmart.util.PasswordUtil;

import java.sql.SQLException;

public final class AuthServiceImpl implements AuthService {
    private static final String GENERIC_LOGIN_FAILURE = "Invalid email or password";
    private final UserDao userDao;

    public AuthServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public AuthenticationResult register(String name, String email, String plaintextPassword) {
        String validName = AuthValidation.validateName(name);
        String validEmail = AuthValidation.validateEmail(email);
        AuthValidation.validatePassword(plaintextPassword);
        try {
            if (userDao.existsByEmail(validEmail)) {
                throw new AuthenticationException("An account already exists for this email");
            }
            User user = new User(validName, validEmail, PasswordUtil.hash(plaintextPassword), UserRole.BUYER);
            return AuthenticationResult.from(userDao.create(user));
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new AuthenticationException("Unable to create account", exception);
        }
    }

    @Override
    public AuthenticationResult login(String email, String plaintextPassword) {
        try {
            String validEmail = AuthValidation.validateEmail(email);
            User user = userDao.findByEmail(validEmail).orElseThrow(() -> new AuthenticationException(GENERIC_LOGIN_FAILURE));
            if (!user.isEnabled() || !PasswordUtil.verify(plaintextPassword, user.getPasswordHash())) {
                throw new AuthenticationException(GENERIC_LOGIN_FAILURE);
            }
            return AuthenticationResult.from(user);
        } catch (AuthenticationException | IllegalArgumentException exception) {
            if (exception instanceof AuthenticationException authenticationException) {
                throw authenticationException;
            }
            throw new AuthenticationException(GENERIC_LOGIN_FAILURE);
        } catch (SQLException exception) {
            throw new AuthenticationException(GENERIC_LOGIN_FAILURE, exception);
        }
    }
}
