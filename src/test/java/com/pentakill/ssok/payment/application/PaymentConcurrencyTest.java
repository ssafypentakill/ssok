package com.pentakill.ssok.payment.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import com.pentakill.ssok.payment.infrastructure.persistence.PaymentAttemptJpaRepository;
import com.pentakill.ssok.payment.infrastructure.persistence.PaymentJpaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// 메일은 보내지 않으므로, 이메일 인증 설정은 스프링이 켜질 수 있을 만큼의 테스트용 값만 넣는다.
@SpringBootTest(properties = {
        "SMTP_HOST=localhost",
        "SMTP_USERNAME=test",
        "SMTP_PASSWORD=test",
        "SMTP_FROM_ADDRESS=test@example.com",
        "EMAIL_VERIFICATION_HMAC_SECRET=cGF5bWVudC1jb25jdXJyZW5jeS10ZXN0LW9ubHktc2VjcmV0"
})
class PaymentConcurrencyTest {

    private static final int REQUEST_COUNT = 10;
    private static final long MEMBER_ID = 10L;
    private static final long AMOUNT = 30_000L;

    @Autowired
    private PaymentApprovalService paymentApprovalService;

    @Autowired
    private PaymentCancelService paymentCancelService;

    @Autowired
    private PaymentJpaRepository paymentJpaRepository;

    @Autowired
    private PaymentAttemptJpaRepository paymentAttemptJpaRepository;

    @Autowired
    private CountingPaymentGateway paymentGateway;

    private Long paymentId;

    @BeforeEach
    void setUp() {
        paymentGateway.reset();
        long orderId = ThreadLocalRandom.current().nextLong(1_000_000_000L, Long.MAX_VALUE);
        paymentId = paymentJpaRepository.save(Payment.ready(orderId, MEMBER_ID, AMOUNT)).getId();
    }

    @AfterEach
    void tearDown() {
        paymentAttemptJpaRepository.findAll().stream()
                .filter(attempt -> attempt.isFor(paymentId))
                .forEach(paymentAttemptJpaRepository::delete);
        paymentJpaRepository.deleteById(paymentId);
    }

    @Test
    @DisplayName("같은 결제를 서로 다른 멱등키로 동시에 10번 승인해도 PG는 한 번만 불리고 하나만 성공한다")
    void approvesOnlyOnceUnderConcurrentRequests() throws Exception {
        List<Boolean> results = runConcurrently(() -> paymentApprovalService.approve(
                paymentId, MEMBER_ID, UUID.randomUUID().toString(), "pay_concurrent", AMOUNT));

        assertThat(paymentGateway.approveCalls()).isEqualTo(1);
        assertThat(results).filteredOn(success -> success).hasSize(1);
        assertThat(paymentJpaRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("같은 멱등키로 동시에 10번 승인하면 PG는 한 번만 불리고, 나머지는 오류 없이 결제 상태를 돌려받는다")
    void replaysSameKeyUnderConcurrentRequests() throws Exception {
        String idempotencyKey = UUID.randomUUID().toString();

        List<Boolean> results = runConcurrently(() -> paymentApprovalService.approve(
                paymentId, MEMBER_ID, idempotencyKey, "pay_concurrent", AMOUNT));

        assertThat(paymentGateway.approveCalls()).isEqualTo(1);
        assertThat(results).containsOnly(true);
        assertThat(paymentAttemptJpaRepository.findByIdempotencyKey(idempotencyKey)).isPresent();
        assertThat(paymentJpaRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    @DisplayName("승인된 결제를 동시에 10번 취소해도 PG 취소는 한 번만 불리고 하나만 성공한다")
    void cancelsOnlyOnceUnderConcurrentRequests() throws Exception {
        paymentApprovalService.approve(paymentId, MEMBER_ID, UUID.randomUUID().toString(), "pay_concurrent", AMOUNT);

        List<Boolean> results = runConcurrently(
                () -> paymentCancelService.cancel(paymentId, MEMBER_ID, "고객 요청"));

        assertThat(paymentGateway.cancelCalls()).isEqualTo(1);
        assertThat(results).filteredOn(success -> success).hasSize(1);
        assertThat(paymentJpaRepository.findById(paymentId).orElseThrow().getStatus())
                .isEqualTo(PaymentStatus.CANCELED);
    }

    private List<Boolean> runConcurrently(Callable<Payment> task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        CountDownLatch ready = new CountDownLatch(REQUEST_COUNT);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < REQUEST_COUNT; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    try {
                        task.call();
                        return true;
                    } catch (IllegalStateException e) {
                        return false;
                    }
                }));
            }
            ready.await();
            start.countDown();

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    static class CountingPaymentGateway implements PaymentGateway {

        private final AtomicInteger approveCalls = new AtomicInteger();
        private final AtomicInteger cancelCalls = new AtomicInteger();

        @Override
        public PgApprovalResult approve(String paymentKey, Long orderId, long amount) {
            approveCalls.incrementAndGet();
            pause();
            return PgApprovalResult.APPROVED;
        }

        @Override
        public void cancel(String paymentKey, String cancelReason) {
            cancelCalls.incrementAndGet();
            pause();
        }

        int approveCalls() {
            return approveCalls.get();
        }

        int cancelCalls() {
            return cancelCalls.get();
        }

        void reset() {
            approveCalls.set(0);
            cancelCalls.set(0);
        }

        private void pause() {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @TestConfiguration
    static class CountingPaymentGatewayConfig {

        @Bean
        @Primary
        CountingPaymentGateway countingPaymentGateway() {
            return new CountingPaymentGateway();
        }
    }
}
