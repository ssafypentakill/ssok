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

    @Column(name = "user_id", nullable = false)
    private Long memberId;

    @Column(name="shipping_address_id", nullable=false)
    private Long deliveryId;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

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

    private Order(
            Long memberId,
            Long deliveryId,
            Long orderItemId,
            long shippingFee,
            long totalAmount
    ) {
        this.memberId = memberId;
        this.deliveryId = deliveryId;
        this.orderItemId = orderItemId;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.deleted = false;
    }

    public static Order create(
            Long memberId,
            Long deliveryId,
            Long orderItemId,
            long shippingFee,
            long totalAmount
    ) {
        return new Order(
                memberId,
                deliveryId,
                orderItemId,
                shippingFee,
                totalAmount
        );
    }
    public void markPaid(){
        if(this.status != OrderStatus.PENDING_PAYMENT){
            throw  new IllegalStateException("결제 대기 주문만 결제 완료로 바꿀 수 있습니다.");
        }
        this.status = OrderStatus.PAID;
    }
    public void markCanceled(){
        if(this.status == OrderStatus.CANCELED){
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELED;
    }
}
