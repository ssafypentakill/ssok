package com.pentakill.ssok.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Entity
@Table(
        name = "payments",
        uniqueConstraints = @UniqueConstraint(name = "uk_payments_order_id", columnNames = "order_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 200)
    private String paymentKey;

    private LocalDateTime approvedAt;

    private LocalDateTime canceledAt;

    @Column(length = 200)
    private String cancelReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Payment(Long orderId, Long memberId, long amount) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.amount = amount;
        this.status = PaymentStatus.READY;
    }

    public static Payment ready(Long orderId, Long memberId, long amount) {
        if (orderId == null || memberId == null) {
            throw new IllegalArgumentException("주문 ID와 회원 ID는 필수입니다.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }
        return new Payment(orderId, memberId, amount);
    }

    public void startApproval(String paymentKey, long requestAmount) {
        if (status != PaymentStatus.READY && status != PaymentStatus.FAILED) {
            throw new IllegalStateException("승인을 시작할 수 없는 상태입니다. 현재 상태: " + status);
        }
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("결제 키는 필수입니다.");
        }
        validateAmount(requestAmount);
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.IN_PROGRESS;
    }

    // 요청 금액을 믿지 않고, 저장된 금액과 같은지 확인한다.
    public void validateAmount(long requestAmount) {
        if (requestAmount != amount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }
    }

    public void approve(LocalDateTime approvedAt) {
        validateInProgress();
        if (approvedAt == null) {
            throw new IllegalArgumentException("승인 시각은 필수입니다.");
        }
        this.approvedAt = approvedAt;
        this.status = PaymentStatus.APPROVED;
    }

    public void fail() {
        validateInProgress();
        this.status = PaymentStatus.FAILED;
    }

    public void markUnknown() {
        validateInProgress();
        this.status = PaymentStatus.UNKNOWN;
    }

    // PG에 취소를 요청하기 전에 먼저 CANCELING으로 바꿔, 동시에 들어온 다른 취소 요청이 PG를 부르지 못하게 한다.
    public void startCancel(String cancelReason) {
        if (status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("승인된 결제만 취소할 수 있습니다. 현재 상태: " + status);
        }
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new IllegalArgumentException("취소 사유는 필수입니다.");
        }
        this.cancelReason = cancelReason;
        this.status = PaymentStatus.CANCELING;
    }

    public void completeCancel(LocalDateTime canceledAt) {
        if (status != PaymentStatus.CANCELING) {
            throw new IllegalStateException("취소 요청 중인 결제가 아닙니다. 현재 상태: " + status);
        }
        if (canceledAt == null) {
            throw new IllegalArgumentException("취소 시각은 필수입니다.");
        }
        this.canceledAt = canceledAt;
        this.status = PaymentStatus.CANCELED;
    }

    private void validateInProgress() {
        if (status != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException("승인 요청 중인 결제가 아닙니다. 현재 상태: " + status);
        }
    }
}
