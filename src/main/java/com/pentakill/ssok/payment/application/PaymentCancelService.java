package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentCancelService {

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public Payment cancel(Long paymentId, Long memberId, String cancelReason) {
        Payment payment = paymentService.startCancel(paymentId, memberId, cancelReason);
        paymentGateway.cancel(payment.getPaymentKey(), cancelReason);
        return paymentService.completeCancel(paymentId, LocalDateTime.now());
    }
}
