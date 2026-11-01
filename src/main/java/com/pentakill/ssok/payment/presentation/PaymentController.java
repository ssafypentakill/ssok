package com.pentakill.ssok.payment.presentation;

import com.pentakill.ssok.payment.application.PaymentApprovalService;
import com.pentakill.ssok.payment.application.PaymentService;
import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import com.pentakill.ssok.payment.presentation.dto.ApprovePaymentRequest;
import com.pentakill.ssok.payment.presentation.dto.PaymentResponse;
import com.pentakill.ssok.payment.presentation.dto.PreparePaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;
    private final PaymentApprovalService paymentApprovalService;

    @PostMapping
    public PaymentResponse prepare(@RequestHeader("X-Member-Id") Long memberId, @RequestBody PreparePaymentRequest request) {
        Payment payment = paymentService.prepare(request.orderId(), memberId);
        return PaymentResponse.from(payment);
    }

    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<PaymentResponse> approve(
            @RequestHeader("X-Member-Id") Long memberId,
            @PathVariable Long paymentId,
            @RequestBody ApprovePaymentRequest request
            ) {
        Payment payment = paymentApprovalService.approve(paymentId, memberId, request.paymentKey(), request.amount());
        HttpStatus status = payment.getStatus() == PaymentStatus.UNKNOWN ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return  ResponseEntity.status(status).body(PaymentResponse.from(payment));
    }
}
