package com.ashokmart.dao.impl;

import com.ashokmart.dao.OrderDao;
import com.ashokmart.model.AdminOrderModels;
import com.ashokmart.model.CheckoutItem;
import com.ashokmart.model.Order;
import com.ashokmart.model.OrderItem;
import com.ashokmart.model.OrderItemView;
import com.ashokmart.model.OrderSummary;
import com.ashokmart.model.SellerOrderSummary;
import com.ashokmart.util.DatabaseConnectionPool;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class OrderDaoImpl implements OrderDao {
    private final DatabaseConnectionPool pool;

    public OrderDaoImpl(DatabaseConnectionPool pool) {
        this.pool = pool;
    }

    @Override
    public List<CheckoutItem> findCheckoutItems(Connection connection, long buyerId) throws SQLException {
        String sql = "SELECT p.id, p.seller_id, p.name, ci.quantity, p.price, p.stock_quantity, p.enabled "
                + "FROM cart c JOIN cart_items ci ON ci.cart_id = c.id "
                + "JOIN products p ON p.id = ci.product_id "
                + "WHERE c.user_id = ? ORDER BY ci.id FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                List<CheckoutItem> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new CheckoutItem(result.getLong("id"), result.getLong("seller_id"),
                            result.getString("name"), result.getInt("quantity"), result.getBigDecimal("price"),
                            result.getInt("stock_quantity"), result.getBoolean("enabled")));
                }
                return items;
            }
        }
    }

    @Override
    public long createOrder(Connection connection, long buyerId, BigDecimal totalAmount) throws SQLException {
        String sql = "INSERT INTO orders (buyer_id, total_amount, status) VALUES (?, ?, 'PENDING')";
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, buyerId);
            statement.setBigDecimal(2, totalAmount);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Order creation did not return an ID");
                return keys.getLong(1);
            }
        }
    }

    @Override
    public void createOrderItem(Connection connection, OrderItem item) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, product_id, seller_id, quantity, unit_price, subtotal) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, item.orderId());
            statement.setLong(2, item.productId());
            statement.setLong(3, item.sellerId());
            statement.setInt(4, item.quantity());
            statement.setBigDecimal(5, item.unitPrice());
            statement.setBigDecimal(6, item.subtotal());
            statement.executeUpdate();
        }
    }

    @Override
    public int decrementStock(Connection connection, long productId, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock_quantity = stock_quantity - ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE id = ? AND enabled = TRUE AND stock_quantity >= ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setLong(2, productId);
            statement.setInt(3, quantity);
            return statement.executeUpdate();
        }
    }

    @Override
    public int clearCart(Connection connection, long buyerId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE cart_id = (SELECT id FROM cart WHERE user_id = ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buyerId);
            return statement.executeUpdate();
        }
    }

    @Override
    public Optional<Order> findOrderByBuyerId(Connection connection, long buyerId, long orderId) throws SQLException {
        String sql = "SELECT o.id, o.buyer_id, o.total_amount, o.status, o.created_at FROM orders o "
                + "WHERE o.id = ? AND o.buyer_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapOrder(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<OrderItemView> findOrderItems(Connection connection, long buyerId, long orderId) throws SQLException {
        String sql = "SELECT oi.product_id, p.name, p.image_url, oi.quantity, oi.unit_price, oi.subtotal "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "JOIN orders o ON o.id = oi.order_id "
                + "WHERE oi.order_id = ? AND o.buyer_id = ? ORDER BY oi.id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderItemView> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new OrderItemView(result.getLong("product_id"), result.getString("name"),
                            result.getString("image_url"), result.getInt("quantity"),
                            result.getBigDecimal("unit_price"), result.getBigDecimal("subtotal")));
                }
                return items;
            }
        }
    }

    @Override
    public List<OrderSummary> findOrdersByBuyerId(long buyerId) throws SQLException {
        String sql = "SELECT o.id, o.total_amount, o.status, o.created_at, COUNT(oi.id) AS item_count "
                + "FROM orders o LEFT JOIN order_items oi ON oi.order_id = o.id "
                + "WHERE o.buyer_id = ? "
                + "GROUP BY o.id, o.total_amount, o.status, o.created_at "
                + "ORDER BY o.created_at DESC, o.id DESC";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, buyerId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderSummary> orders = new ArrayList<>();
                while (result.next()) {
                    orders.add(new OrderSummary(result.getLong("id"), result.getBigDecimal("total_amount"),
                            result.getString("status"), result.getTimestamp("created_at").toLocalDateTime(),
                            result.getLong("item_count")));
                }
                return orders;
            }
        }
    }

    @Override
    public Optional<Order> findOrderByIdAndBuyerId(long orderId, long buyerId) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            return findOrderByBuyerId(connection, buyerId, orderId);
        }
    }

    @Override
    public List<OrderItemView> findOrderItems(long orderId, long buyerId) throws SQLException {
        try (Connection connection = pool.getConnection()) {
            return findOrderItems(connection, buyerId, orderId);
        }
    }

    @Override
    public List<SellerOrderSummary> findOrdersForSeller(long sellerId) throws SQLException {
        String sql = "SELECT o.id, SUM(oi.subtotal) AS seller_subtotal, o.status, o.created_at, COUNT(oi.id) AS item_count "
                + "FROM orders o JOIN order_items oi ON oi.order_id = o.id "
                + "JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.seller_id = ? AND p.seller_id = ? "
                + "GROUP BY o.id, o.status, o.created_at ORDER BY o.created_at DESC, o.id DESC";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, sellerId);
            statement.setLong(2, sellerId);
            try (ResultSet result = statement.executeQuery()) {
                List<SellerOrderSummary> orders = new ArrayList<>();
                while (result.next()) {
                    orders.add(new SellerOrderSummary(result.getLong("id"), result.getBigDecimal("seller_subtotal"),
                            result.getString("status"), result.getTimestamp("created_at").toLocalDateTime(),
                            result.getLong("item_count")));
                }
                return orders;
            }
        }
    }

    @Override
    public Optional<Order> findOrderForSeller(long orderId, long sellerId) throws SQLException {
        String sql = "SELECT DISTINCT o.id, o.buyer_id, o.total_amount, o.status, o.created_at FROM orders o "
                + "JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id "
                + "WHERE o.id = ? AND oi.seller_id = ? AND p.seller_id = ?";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, sellerId);
            statement.setLong(3, sellerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapOrder(result)) : Optional.empty();
            }
        }
    }

    @Override
    public List<OrderItemView> findSellerOrderItems(long orderId, long sellerId) throws SQLException {
        String sql = "SELECT oi.product_id, p.name, p.image_url, oi.quantity, oi.unit_price, oi.subtotal "
                + "FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.order_id = ? AND oi.seller_id = ? AND p.seller_id = ? ORDER BY oi.id";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setLong(2, sellerId);
            statement.setLong(3, sellerId);
            try (ResultSet result = statement.executeQuery()) {
                List<OrderItemView> items = new ArrayList<>();
                while (result.next()) {
                    items.add(new OrderItemView(result.getLong("product_id"), result.getString("name"),
                            result.getString("image_url"), result.getInt("quantity"),
                            result.getBigDecimal("unit_price"), result.getBigDecimal("subtotal")));
                }
                return items;
            }
        }
    }

    @Override
    public boolean updateSellerOrderStatus(long orderId, long sellerId, String status) throws SQLException {
        String sql = "UPDATE orders SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? "
                + "AND EXISTS (SELECT 1 FROM order_items oi JOIN products p ON p.id = oi.product_id "
                + "WHERE oi.order_id = orders.id AND oi.seller_id = ? AND p.seller_id = ?)";
        try (Connection connection = pool.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setLong(2, orderId);
            statement.setLong(3, sellerId);
            statement.setLong(4, sellerId);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public List<AdminOrderModels.Summary> findAdminOrders(AdminOrderModels.Query query) throws SQLException {
        List<Object> params = new ArrayList<>();
        String sql = "SELECT o.id, u.name buyer_name, u.email buyer_email, o.total_amount, o.status, o.created_at, COUNT(oi.id) item_count "
                + "FROM orders o JOIN users u ON u.id=o.buyer_id LEFT JOIN order_items oi ON oi.order_id=o.id" + adminOrderWhere(query, params)
                + " GROUP BY o.id,u.name,u.email,o.total_amount,o.status,o.created_at ORDER BY o.created_at DESC,o.id DESC LIMIT ? OFFSET ?";
        params.add(query.pageSize()); params.add(query.offset());
        try (Connection c=pool.getConnection(); PreparedStatement s=c.prepareStatement(sql)) { bindAdmin(s, params); try(ResultSet r=s.executeQuery()){ List<AdminOrderModels.Summary> out=new ArrayList<>(); while(r.next()) out.add(new AdminOrderModels.Summary(r.getLong("id"),r.getString("buyer_name"),r.getString("buyer_email"),r.getBigDecimal("total_amount"),r.getString("status"),r.getTimestamp("created_at").toLocalDateTime(),r.getLong("item_count"))); return out; } }
    }

    @Override
    public long countAdminOrders(AdminOrderModels.Query query) throws SQLException {
        List<Object> params = new ArrayList<>(); String sql="SELECT COUNT(*) FROM orders o JOIN users u ON u.id=o.buyer_id"+adminOrderWhere(query,params);
        try(Connection c=pool.getConnection(); PreparedStatement s=c.prepareStatement(sql)){bindAdmin(s,params);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}
    }

    @Override
    public Optional<AdminOrderModels.Details> findAdminOrderDetails(long orderId) throws SQLException {
        if(orderId<=0)return Optional.empty();
        String orderSql="SELECT o.id,o.buyer_id,o.total_amount,o.status,o.created_at,u.name buyer_name,u.email buyer_email FROM orders o JOIN users u ON u.id=o.buyer_id WHERE o.id=?";
        try(Connection c=pool.getConnection(); PreparedStatement s=c.prepareStatement(orderSql)){s.setLong(1,orderId);try(ResultSet r=s.executeQuery()){if(!r.next())return Optional.empty();Order order=mapOrder(r);String buyer=r.getString("buyer_name"), email=r.getString("buyer_email");
            String itemSql="SELECT oi.product_id,p.name product_name,s.name seller_name,s.email seller_email,oi.quantity,oi.unit_price,oi.subtotal FROM order_items oi JOIN products p ON p.id=oi.product_id JOIN users s ON s.id=oi.seller_id WHERE oi.order_id=? ORDER BY oi.id";
            List<AdminOrderModels.Item> items=new ArrayList<>(); try(PreparedStatement is=c.prepareStatement(itemSql)){is.setLong(1,orderId);try(ResultSet ir=is.executeQuery()){while(ir.next())items.add(new AdminOrderModels.Item(ir.getLong("product_id"),ir.getString("product_name"),ir.getString("seller_name"),ir.getString("seller_email"),ir.getInt("quantity"),ir.getBigDecimal("unit_price"),ir.getBigDecimal("subtotal")));}}
            return Optional.of(new AdminOrderModels.Details(order,buyer,email,items));}}
    }

    @Override public long countOrders() throws SQLException { return aggregate("SELECT COUNT(*) FROM orders", null); }
    @Override public long countOrdersByStatus(String status) throws SQLException { return aggregate("SELECT COUNT(*) FROM orders WHERE status = ?", status); }
    @Override public BigDecimal totalRevenue() throws SQLException { return decimalAggregate("SELECT COALESCE(SUM(total_amount),0) FROM orders"); }
    @Override public BigDecimal averageOrderValue() throws SQLException { return decimalAggregate("SELECT COALESCE(AVG(total_amount),0) FROM orders"); }

    private String adminOrderWhere(AdminOrderModels.Query query,List<Object> params){StringBuilder w=new StringBuilder(" WHERE 1=1");if(query.search()!=null){w.append(" AND (CAST(o.id AS VARCHAR) LIKE ? OR LOWER(u.name) LIKE ? OR LOWER(u.email) LIKE ?)");String v="%"+query.search().toLowerCase()+"%";params.add(v);params.add(v);params.add(v);}if(query.status()!=null){w.append(" AND o.status=?");params.add(query.status());}return w.toString();}
    private void bindAdmin(PreparedStatement s,List<Object> params)throws SQLException{for(int i=0;i<params.size();i++){Object p=params.get(i);if(p instanceof String v)s.setString(i+1,v);else s.setObject(i+1,p);}}
    private long aggregate(String sql,String value)throws SQLException{try(Connection c=pool.getConnection();PreparedStatement s=c.prepareStatement(sql)){if(value!=null)s.setString(1,value);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}}
    private BigDecimal decimalAggregate(String sql)throws SQLException{try(Connection c=pool.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){r.next();BigDecimal value=r.getBigDecimal(1);return value==null?BigDecimal.ZERO:value.setScale(2,java.math.RoundingMode.HALF_UP);}}

    private Order mapOrder(ResultSet result) throws SQLException {
        return new Order(result.getLong("id"), result.getLong("buyer_id"),
                result.getBigDecimal("total_amount"), result.getString("status"),
                result.getTimestamp("created_at").toLocalDateTime());
    }
}
