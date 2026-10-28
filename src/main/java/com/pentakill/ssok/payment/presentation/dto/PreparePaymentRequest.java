package com.pentakill.ssok.payment.presentation.dto;

public record PreparePaymentRequest(Long orderId) {
    public PreparePaymentRequest {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }
    }
}
