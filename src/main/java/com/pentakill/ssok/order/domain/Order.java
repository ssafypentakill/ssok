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

    @Column(name="shipping_address_id", nullable=false)
    private Long deliveryId;

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
            long shippingFee,
            long totalAmount
    ) {
        this.memberId = memberId;
        this.deliveryId = deliveryId;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.deleted = false;
    }

    // 결제 대기 상태의 새 주문을 만든다.
    public static Order create(
            Long memberId,
            Long deliveryId,
            long shippingFee,
            long totalAmount
    ) {
        return new Order(
                memberId,
                deliveryId,
                shippingFee,
                totalAmount
        );
    }
    // 결제 대기 주문을 결제 완료로 변경한다.
    public void markPaid(){
        if(this.status != OrderStatus.PENDING_PAYMENT){
            throw  new IllegalStateException("결제 대기 주문만 결제 완료로 바꿀 수 있습니다.");
        }
        this.status = OrderStatus.PAID;
    }
    // 결제 완료 주문을 취소 상태로 변경한다.
    public void markCanceled(){
        if(this.status != OrderStatus.PAID){
            throw new IllegalStateException("결제 완료된 주문만 취소할 수 있습니다.");
        }
        this.status = OrderStatus.CANCELED;
    }
}
