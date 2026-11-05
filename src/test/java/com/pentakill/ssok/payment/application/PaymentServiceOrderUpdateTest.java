package com.pentakill.ssok.payment.application;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentServiceOrderUpdateTest {
    private final OrderReader orderReader = mock(OrderReader.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final OrderPaymentUpdater orderUpdater = mock(OrderPaymentUpdater.class);
    private final PaymentService service = new PaymentService(orderReader, paymentRepository, orderUpdater);

    @Test
    void approvedPaymentMarksOrderPaid() {
        Payment payment = inProgressPayment();

        service.completeApproval(7L, PgApprovalResult.APPROVED, LocalDateTime.now());

        assertEquals(com.pentakill.ssok.payment.domain.PaymentStatus.APPROVED, payment.getStatus());
        verify(orderUpdater).markPaid(90L);
    }

    @Test
    void failedPaymentDoesNotMarkOrderPaid() {
        inProgressPayment();

        service.completeApproval(7L, PgApprovalResult.REJECTED, LocalDateTime.now());

        verify(orderUpdater, never()).markPaid(anyLong());
    }

    private Payment inProgressPayment() {
        Payment payment = Payment.ready(90L, 1L, 50_000L);
        payment.startApproval("pg-key", 50_000L);
        when(paymentRepository.findById(7L)).thenReturn(Optional.of(payment));
        return payment;
    }
}
