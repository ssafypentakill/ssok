package com.pentakill.ssok.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 승인 요청 한 번(시도 한 번)의 기록. 같은 멱등키로 다시 온 요청은 이 기록으로 알아본다.
@Getter
@Entity
@Table(
        name = "payment_attempts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_attempts_idempotency_key",
                columnNames = "idempotency_key"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentAttempt {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, length = MAX_IDEMPOTENCY_KEY_LENGTH)
    private String idempotencyKey;

    @Column(nullable = false, length = 200)
    private String paymentKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentAttemptStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime completedAt;

    private PaymentAttempt(Payment payment, String idempotencyKey, String paymentKey, LocalDateTime requestedAt) {
        this.payment = payment;
        this.idempotencyKey = idempotencyKey;
        this.paymentKey = paymentKey;
        this.requestedAt = requestedAt;
        this.status = PaymentAttemptStatus.IN_PROGRESS;
    }

    public static PaymentAttempt start(
            Payment payment,
            String idempotencyKey,
            String paymentKey,
            LocalDateTime requestedAt
    ) {
        if (payment == null) {
            throw new IllegalArgumentException("결제는 필수입니다.");
        }
        validateIdempotencyKey(idempotencyKey);
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new IllegalArgumentException("결제 키는 필수입니다.");
        }
        if (requestedAt == null) {
            throw new IllegalArgumentException("요청 시각은 필수입니다.");
        }
        return new PaymentAttempt(payment, idempotencyKey, paymentKey, requestedAt);
    }

    public static void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("멱등키는 필수입니다.");
        }
        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new IllegalArgumentException("멱등키는 " + MAX_IDEMPOTENCY_KEY_LENGTH + "자 이하여야 합니다.");
        }
    }

    public boolean isFor(Long paymentId) {
        return payment.getId().equals(paymentId);
    }

    public void approve(LocalDateTime completedAt) {
        complete(PaymentAttemptStatus.APPROVED, completedAt);
    }

    public void fail(LocalDateTime completedAt) {
        complete(PaymentAttemptStatus.FAILED, completedAt);
    }

    public void markUnknown(LocalDateTime completedAt) {
        complete(PaymentAttemptStatus.UNKNOWN, completedAt);
    }

    private void complete(PaymentAttemptStatus result, LocalDateTime completedAt) {
        if (status != PaymentAttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("승인 요청 중인 시도가 아닙니다. 현재 상태: " + status);
        }
        if (completedAt == null) {
            throw new IllegalArgumentException("응답 시각은 필수입니다.");
        }
        this.completedAt = completedAt;
        this.status = result;
    }
}
