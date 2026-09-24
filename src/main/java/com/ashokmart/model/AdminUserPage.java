package com.ashokmart.model;

import java.util.List;

public record AdminUserPage(List<AdminUserView> users, int page, int pageSize, long totalUsers, int totalPages) {
    public List<AdminUserView> getUsers() { return users; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
    public long getTotalUsers() { return totalUsers; }
    public int getTotalPages() { return totalPages; }
    public boolean isHasPrevious() { return page > 1; }
    public boolean isHasNext() { return page < totalPages; }
}
