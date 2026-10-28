package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class FakePaymentRepository implements PaymentRepository {

    private final List<Payment> payments = new ArrayList<>();

    @Override
    public Payment save(Payment payment) {
        if (!payments.contains(payment)) {
            payments.add(payment);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return payments.stream()
                .filter(payment -> id.equals(payment.getId()))
                .findFirst();
    }

    @Override
    public Optional<Payment> findByOrderId(Long orderId) {
        return payments.stream()
                .filter(payment -> orderId.equals(payment.getOrderId()))
                .findFirst();
    }

    int count() {
        return payments.size();
    }
}
