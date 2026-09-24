package com.ashokmart.model;

import java.time.LocalDateTime;

/** Safe administrator-facing user projection; it deliberately contains no password hash. */
public record AdminUserView(long id, String name, String email, UserRole role,
                            boolean enabled, LocalDateTime createdAt) {
    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public UserRole getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
