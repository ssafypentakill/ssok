package com.pentakill.ssok.product.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private long id;

    @Column(name = "product_name", nullable = false, updatable = true)
    private String name;

    @Column(name = "description", nullable = false, updatable = true)
    private String description;

    @Column(name = "brand", nullable = true, updatable = true)
    private String brand;

    @Column(name = "category", nullable = false, updatable = true)
    private String category;

    @Column(name = "price", nullable = false, updatable = true)
    private long price;

    @Column(name = "stock_quantity", nullable = false, updatable = true)
    private long stockQuantity;

    @Column(name = "image_url", nullable = false, updatable = true)
    private String imageUrl;

    @Column(name = "status", nullable = false, updatable = true)
    private String status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updatedAt", nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    public Product update(String name, String description, String brand, String category, long price, String imageUrl){
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.imageUrl = imageUrl;
        return this;
    }
    public void updateQuantity(long quantity){
        this.stockQuantity += quantity;
    }
    public void delete(){
        this.status = "DELETED";
    }
}
