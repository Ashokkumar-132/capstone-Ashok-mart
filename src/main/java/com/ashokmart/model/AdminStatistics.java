package com.ashokmart.model;

public record AdminStatistics(long totalUsers, long buyers, long sellers, long admins,
                              long activeUsers, long inactiveUsers) {
    public long getTotalUsers() { return totalUsers; }
    public long getBuyers() { return buyers; }
    public long getSellers() { return sellers; }
    public long getAdmins() { return admins; }
    public long getActiveUsers() { return activeUsers; }
    public long getInactiveUsers() { return inactiveUsers; }
}
