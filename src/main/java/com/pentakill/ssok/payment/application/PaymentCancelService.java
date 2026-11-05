package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final OrderPaymentUpdater orderPaymentUpdater;

    // 결제 ID로 취소하고 연결된 주문 상태와 재고도 복구한다.
    @Transactional
    public Payment cancel(Long paymentId, Long memberId, String cancelReason) {
        Payment payment = paymentService.findCancelablePayment(paymentId, memberId);
        return cancelApprovedPayment(payment, memberId, cancelReason);
    }

    // 주문 ID로 취소한다. 주문 API에서 사용한다.
    @Transactional
    public Payment cancelByOrderId(Long orderId, Long memberId, String cancelReason) {
        Payment payment = paymentService.findCancelablePaymentByOrderId(orderId, memberId);
        return cancelApprovedPayment(payment, memberId, cancelReason);
    }

    private Payment cancelApprovedPayment(Payment payment, Long memberId, String cancelReason) {
        if (cancelReason == null || cancelReason.isBlank() || cancelReason.length() > 200) {
            throw new IllegalArgumentException("취소 사유는 1~200자여야 합니다.");
        }
        // PG 취소 전에 본인의 결제 완료 주문인지 확인한다.
        orderPaymentUpdater.requirePaidOrder(payment.getOrderId(), memberId);
        paymentGateway.cancel(payment.getPaymentKey(), cancelReason);
        Payment canceled = paymentService.completeCancel(payment.getId(), cancelReason, LocalDateTime.now());
        // 주문 취소와 재고 복구는 결제 상태 변경과 같은 DB 트랜잭션에 포함한다.
        orderPaymentUpdater.cancelAndRestore(payment.getOrderId(), memberId);
        return canceled;
    }
}
