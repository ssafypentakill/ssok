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

    public Payment approve(Long paymentId, Long memberId, String paymentKey, long amount) {
        Payment payment = paymentService.startApproval(paymentId, memberId, paymentKey, amount);
        PgApprovalResult result = paymentGateway. approve(paymentKey, payment.getOrderId(), amount);
        return paymentService. completeApproval(paymentId, result, LocalDateTime.now());
    }
}
