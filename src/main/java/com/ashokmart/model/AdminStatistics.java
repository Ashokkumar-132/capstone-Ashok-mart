package com.ashokmart.model;

import java.math.BigDecimal;

public record AdminStatistics(long totalUsers, long buyers, long sellers, long admins, long activeUsers, long inactiveUsers,
                              long totalProducts, long activeProducts, long inactiveProducts, long inStockProducts, long outOfStockProducts,
                              long totalOrders, BigDecimal totalRevenue, BigDecimal averageOrderValue) {
    public AdminStatistics(long totalUsers,long buyers,long sellers,long admins,long activeUsers,long inactiveUsers){this(totalUsers,buyers,sellers,admins,activeUsers,inactiveUsers,0,0,0,0,0,0,BigDecimal.ZERO,BigDecimal.ZERO);}
    public long getTotalUsers(){return totalUsers;} public long getBuyers(){return buyers;} public long getSellers(){return sellers;} public long getAdmins(){return admins;} public long getActiveUsers(){return activeUsers;} public long getInactiveUsers(){return inactiveUsers;}
    public long getTotalProducts(){return totalProducts;} public long getActiveProducts(){return activeProducts;} public long getInactiveProducts(){return inactiveProducts;} public long getInStockProducts(){return inStockProducts;} public long getOutOfStockProducts(){return outOfStockProducts;}
    public long getTotalOrders(){return totalOrders;} public BigDecimal getTotalRevenue(){return totalRevenue;} public BigDecimal getAverageOrderValue(){return averageOrderValue;}
}
