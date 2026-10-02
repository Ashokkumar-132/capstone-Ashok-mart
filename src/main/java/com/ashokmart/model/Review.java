package com.ashokmart.model;
import java.time.LocalDateTime;
public record Review(long id,long productId,long buyerId,int rating,String comment,LocalDateTime createdAt,LocalDateTime updatedAt,String buyerName){
 public Review(long id,long productId,long buyerId,int rating,String comment,LocalDateTime createdAt,LocalDateTime updatedAt){this(id,productId,buyerId,rating,comment,createdAt,updatedAt,null);}
 public long getId(){return id;} public long getProductId(){return productId;} public long getBuyerId(){return buyerId;} public int getRating(){return rating;} public String getComment(){return comment;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;} public String getBuyerName(){return buyerName;}
}
