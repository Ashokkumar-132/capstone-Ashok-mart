package com.ashokmart.dao;
import com.ashokmart.model.Review; import java.math.BigDecimal; import java.sql.SQLException; import java.util.List; import java.util.Optional;
public interface ReviewDao {
 List<Review> findByProductId(long productId) throws SQLException; Optional<Review> findById(long reviewId) throws SQLException; Optional<Review> findByBuyerAndProduct(long buyerId,long productId) throws SQLException; long create(Review review) throws SQLException; boolean update(Review review,long buyerId) throws SQLException; boolean delete(long reviewId,long buyerId) throws SQLException; long countByProductId(long productId) throws SQLException; BigDecimal averageRating(long productId) throws SQLException; boolean hasPurchasedProduct(long buyerId,long productId) throws SQLException;
}
