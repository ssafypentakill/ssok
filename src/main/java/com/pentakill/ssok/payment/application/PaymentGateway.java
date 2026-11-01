package com.pentakill.ssok.payment.application;

public interface PaymentGateway {
    PgApprovalResult approve(String paymentKey, Long orderId, long amount);
}
