package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class FakePaymentRepository implements PaymentRepository {

    private final List<Payment> payments = new ArrayList<>();
    private long nextId = 1L;

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            ReflectionTestUtils.setField(payment, "id", nextId++);
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

    @Override
    public Optional<Payment> findByIdForUpdate(Long id) {
        return findById(id);
    }

    @Override
    public Optional<Payment> findByOrderIdForUpdate(Long orderId) {
        return findByOrderId(orderId);
    }

    int count() {
        return payments.size();
    }
}
