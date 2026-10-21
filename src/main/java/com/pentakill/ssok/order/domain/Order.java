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

    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 30)
    private String recipientPhone;

    @Column(name = "postal_code", nullable = false, length = 10)
    private String postalCode;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "address_detail", length = 255)
    private String addressDetail;

    @Column(name = "shipping_fee", nullable = false)
    private long shippingFee;

    @Column(name = "total_amount", nullable = false)
    private long totalAmount;

    private Order(
            Long memberId,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address,
            String addressDetail,
            long shippingFee,
            long totalAmount
    ) {
        this.memberId = memberId;
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.shippingFee = shippingFee;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.deleted = false;
    }

    public static Order create(
            Long memberId,
            String recipientName,
            String recipientPhone,
            String postalCode,
            String address,
            String addressDetail,
            long shippingFee,
            long totalAmount
    ) {
        return new Order(
                memberId,
                recipientName,
                recipientPhone,
                postalCode,
                address,
                addressDetail,
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