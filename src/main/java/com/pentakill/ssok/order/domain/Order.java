package com.pentakill.ssok.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;


    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 20)
    private OrderStatus status;

    @CreationTimestamp
    @Column(name = "order_datetime", nullable = false, updatable = false)
    private LocalDateTime orderAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean deleted;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    private Order(Long memberId, OrderStatus status, long shippingFee, long totalAmount, LocalDateTime orderAt, Boolean Deleted) {
        this.memberId = memberId;
        this.orderAt = orderAt;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING_PAYMENT; // 처음에는 결제 대기
        this.deleted = false; // 처음에는 삭제되지 않은 주문

    }
}