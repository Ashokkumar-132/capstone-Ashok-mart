package com.ashokmart.model;

public record AdminUserQuery(String search, UserRole role, Boolean enabled, int page, int pageSize) {
    public AdminUserQuery {
        search = search == null ? "" : search.trim();
        page = Math.max(1, page);
        pageSize = Math.min(50, Math.max(5, pageSize));
    }
    public String getSearch() { return search; }
    public UserRole getRole() { return role; }
    public Boolean getEnabled() { return enabled; }
    public int getPage() { return page; }
    public int getPageSize() { return pageSize; }
}
