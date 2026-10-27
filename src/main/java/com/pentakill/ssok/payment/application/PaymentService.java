package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    private Payment validateReusable(Payment payment) {
        PaymentStatus status = payment.getStatus();
        if (status == PaymentStatus.APPROVED || status == PaymentStatus.CANCELED) {
            throw new IllegalStateException("이미 처리된 결제입니다.");
        }
        return payment;
    }
}
