package com.ashokmart.service;
import com.ashokmart.model.*;
import java.util.Optional;
public interface AdminOrderService {
    AdminOrderPage getOrders(long adminId, AdminOrderModels.Query query);
    Optional<AdminOrderModels.Details> getOrder(long adminId, long orderId);
}
