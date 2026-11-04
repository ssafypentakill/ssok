package com.pentakill.ssok.payment.presentation.dto;

public record CancelPaymentRequest(String cancelReason) {

    public CancelPaymentRequest {
        if (cancelReason == null || cancelReason.isBlank()) {
            throw new IllegalArgumentException("취소 사유는 필수입니다.");
        }
    }
}
