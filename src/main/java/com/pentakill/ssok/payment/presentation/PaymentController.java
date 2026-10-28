package com.pentakill.ssok.payment.presentation;

import com.pentakill.ssok.payment.application.PaymentService;
import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.presentation.dto.PaymentResponse;
import com.pentakill.ssok.payment.presentation.dto.PreparePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public PaymentResponse prepare(@RequestHeader("X-Member-Id") Long memberId, @RequestBody PreparePaymentRequest request) {
        Payment payment = paymentService.prepare(request.orderId(), memberId);
        return PaymentResponse.from(payment);
    }
}
