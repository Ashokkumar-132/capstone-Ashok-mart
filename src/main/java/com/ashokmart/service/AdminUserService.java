package com.ashokmart.service;

import com.ashokmart.model.AdminStatistics;
import com.ashokmart.model.AdminUserPage;
import com.ashokmart.model.AdminUserQuery;
import com.ashokmart.model.AdminUserView;

import java.util.Optional;

public interface AdminUserService {
    AdminStatistics getStatistics(long authenticatedAdminId);
    AdminUserPage getUsers(long authenticatedAdminId, AdminUserQuery query);
    Optional<AdminUserView> getUser(long authenticatedAdminId, long userId);
    void activateUser(long authenticatedAdminId, long userId);
    void deactivateUser(long authenticatedAdminId, long userId);
}
