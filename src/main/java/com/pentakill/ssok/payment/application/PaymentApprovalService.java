package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentApprovalService {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public Payment approve(Long paymentId, Long memberId, String idempotencyKey, String paymentKey, long amount) {
        ApprovalStart start = paymentService.startApproval(
                paymentId, memberId, idempotencyKey, paymentKey, amount, LocalDateTime.now());
        if (start.replayed()) {
            return start.payment();
        }
        PgApprovalResult result = paymentGateway.approve(paymentKey, start.payment().getOrderId(), amount);
        return paymentService.completeApproval(paymentId, start.attemptId(), result, LocalDateTime.now());
    }
}
