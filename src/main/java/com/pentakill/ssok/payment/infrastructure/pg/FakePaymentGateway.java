package com.pentakill.ssok.payment.infrastructure.pg;

import com.pentakill.ssok.payment.application.PaymentGateway;
import com.pentakill.ssok.payment.application.PgApprovalResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

@Component
public class FakePaymentGateway implements PaymentGateway {

    private final double rejectRate;
    private final double unknownRate;
    private final long delayMillis;
    private final DoubleSupplier randomSource;

    @Autowired
    public FakePaymentGateway(
            @Value("${payment.fake-pg.reject-rate:0}") double rejectRate,
            @Value("${payment.fake-pg.unknown-rate:0}") double unknownRate,
            @Value("${payment.fake-pg.delay-millis:0}") long delayMillis
    ) {
        this(rejectRate, unknownRate, delayMillis, () -> ThreadLocalRandom.current().nextDouble());
    }

    FakePaymentGateway(double rejectRate, double unknownRate, long delayMillis, DoubleSupplier randomSource) {
        this.rejectRate = rejectRate;
        this.unknownRate = unknownRate;
        this.delayMillis = delayMillis;
        this.randomSource = randomSource;
    }

    @Override
    public PgApprovalResult approve(String paymentKey, Long orderId, long amount) {
        if (!waitForResponse()) {
            return PgApprovalResult.UNKNOWN;
        }

        double chance = randomSource.getAsDouble();
        if (chance < unknownRate) {
            return PgApprovalResult.UNKNOWN;
        }
        if (chance < unknownRate + rejectRate) {
            return PgApprovalResult.REJECTED;
        }
        return PgApprovalResult.APPROVED;
    }

    private boolean waitForResponse() {
        try {
            Thread.sleep(delayMillis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
