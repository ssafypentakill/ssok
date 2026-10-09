package com.pentakill.ssok.payment.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Payment {
    private Long orderId;
    private Long memberId;
    private long amount;
    private PaymentStatus status;
    private String paymentKey;
    private LocalDateTime approvedAt;

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
        if (requestAmount != amount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.IN_PROGRESS;
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

    private void validateInProgress() {
        if (status != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException("승인 요청 중인 결제가 아닙니다. 현재 상태: " + status);
        }
    }
}
