package com.pentakill.ssok.payment.application;

public interface OrderPaymentUpdater {
    void markPaid(Long orderId);
    void requirePaidOrder(Long orderId, Long memberId);
    void cancelAndRestore(Long orderId, Long memberId);
}
