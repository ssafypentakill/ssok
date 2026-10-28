package com.pentakill.ssok.payment.presentation.dto;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;

public record PaymentResponse(Long paymentId, Long orderId, long amount, PaymentStatus status) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus()
        );
    }
}
