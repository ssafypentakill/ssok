package com.pentakill.ssok.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private OrderItem(
            Long orderId,
            Long productId,
            int quantity,
            String productName,
            long unitPrice
    ) {
        if (orderId == null || productId == null) {
            throw new IllegalArgumentException("주문 ID와 상품 ID는 필수입니다.");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1개 이상이어야 합니다.");
        }
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("상품명은 필수입니다.");
        }
        if (unitPrice < 0) {
            throw new IllegalArgumentException("상품 가격이 올바르지 않습니다.");
        }

        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.productName = productName;
        this.unitPrice = unitPrice;
    }

    // 주문 당시 상품명, 단가, 수량을 보존하는 주문상품을 만든다.
    public static OrderItem create(
            Long orderId,
            Long productId,
            int quantity,
            String productName,
            long unitPrice
    ) {
        return new OrderItem(orderId, productId, quantity, productName, unitPrice);
    }

    // 이 주문상품의 구매 금액을 계산한다.
    public long getSubtotal() {
        return Math.multiplyExact(unitPrice, (long) quantity);
    }
}
