package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderReader orderReader;
    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment prepare(Long orderId, Long memberId) {
        OrderInfo order = orderReader.findOrder(orderId)
                .filter(found -> found.isOrderedBy(memberId))
                .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다."));

        if (!order.payable()) {
            throw new IllegalStateException("결제할 수 없는 주문입니다.");
        }

        return paymentRepository.findByOrderId(orderId)
                .map(this::validateReusable)
                .orElseGet(() -> paymentRepository.save(Payment.ready(orderId, memberId, order.amount())));
    }

    @Transactional
    public Payment startApproval(Long paymentId, Long memberId, String paymentKey, long amount) {
        Payment payment = findOwnedPaymentForUpdate(paymentId, memberId);
        payment.startApproval(paymentKey, amount);
        return payment;
    }

    @Transactional
    public Payment completeApproval(Long paymentId, PgApprovalResult result, LocalDateTime approvedAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다."));

        switch (result) {
            case APPROVED -> payment.approve(approvedAt);
            case REJECTED -> payment.fail();
            case UNKNOWN -> payment.markUnknown();
            case null -> throw  new IllegalArgumentException("PG 결과는 필수입니다.");
        }
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long paymentId, Long memberId) {
        return findOwnedPayment(paymentId, memberId);
    }

    @Transactional
    public Payment startCancel(Long paymentId, Long memberId, String cancelReason) {
        Payment payment = findOwnedPaymentForUpdate(paymentId, memberId);
        payment.startCancel(cancelReason);
        return payment;
    }

    @Transactional
    public Payment completeCancel(Long paymentId, LocalDateTime canceledAt) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다."));
        payment.completeCancel(canceledAt);
        return payment;
    }

    private Payment findOwnedPayment(Long paymentId, Long memberId) {
        return paymentRepository.findById(paymentId)
                .filter(payment -> payment.getMemberId().equals(memberId))
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다."));
    }

    private Payment findOwnedPaymentForUpdate(Long paymentId, Long memberId) {
        return paymentRepository.findByIdForUpdate(paymentId)
                .filter(payment -> payment.getMemberId().equals(memberId))
                .orElseThrow(() -> new EntityNotFoundException("결제를 찾을 수 없습니다."));
    }

    private Payment validateReusable(Payment payment) {
        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.APPROVED
                || status == PaymentStatus.CANCELING
                || status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 처리된 결제입니다.");
        }
        return payment;
    }
}
