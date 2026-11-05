package com.pentakill.ssok.payment.application;

// 결제 흐름에서 주문 상태와 재고를 갱신하기 위한 연결 지점이다.
public interface OrderPaymentUpdater {
    void markPaid(Long orderId);
    void requirePaidOrder(Long orderId, Long memberId);
    void cancelAndRestore(Long orderId, Long memberId);
}
