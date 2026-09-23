package com.ashokmart.model;

/** Safe web-layer authentication data; it intentionally contains no password hash. */
public record AuthenticationResult(long userId, String name, String email, UserRole role) {
    public static AuthenticationResult from(User user) {
        return new AuthenticationResult(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
