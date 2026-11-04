package com.pentakill.ssok.cart.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "cart_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_item_cart_product",
                columnNames = {"cart_id", "product_id"}
        )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
    private long id;

    @Column(name = "cart_id")
    private long cartId;

    @Column(name = "product_id")
    private long productId;

    @Column(name = "quantity")
    private int quantity;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, updatable = true)
    private LocalDateTime updatedAt;

    public CartItem(Long cartId, Long productId, int finalQuantity) {
        this.cartId = cartId;
        this.productId = productId;
        changeQuantity(finalQuantity);
    }

    public void changeQuantity(int finalQuantity) {
        if (finalQuantity < 1 || finalQuantity > 99) {
            throw new IllegalArgumentException("수량은 1 이상 99 이하여야 합니다.");
        }
        this.quantity = finalQuantity;
    }
}
