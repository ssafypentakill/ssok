package com.pentakill.ssok.payment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pentakill.ssok.payment.domain.Payment;
import com.pentakill.ssok.payment.domain.PaymentRepository;
import com.pentakill.ssok.payment.domain.PaymentStatus;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PaymentRepositoryImpl.class)
class PaymentRepositoryImplTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("결제를 저장하면 id가 생기고, DB에서 다시 꺼내도 값이 같다")
    void savesAndFindsById() {
        Payment saved = paymentRepository.save(Payment.ready(101L, 1L, 30_000L));
        entityManager.flush();
        entityManager.clear();

        Payment found = paymentRepository.findById(saved.getId()).orElseThrow();

        assertThat(saved.getId()).isNotNull();
        assertThat(found).isNotSameAs(saved);
        assertThat(found.getOrderId()).isEqualTo(101L);
        assertThat(found.getMemberId()).isEqualTo(1L);
        assertThat(found.getAmount()).isEqualTo(30_000L);
        assertThat(found.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("결제 상태는 DB에 숫자가 아니라 이름 그대로 저장된다")
    void storesStatusAsString() {
        Payment saved = paymentRepository.save(Payment.ready(101L, 1L, 30_000L));
        entityManager.flush();

        Object status = entityManager.getEntityManager()
                .createNativeQuery("SELECT status FROM payments WHERE id = :id")
                .setParameter("id", saved.getId())
                .getSingleResult();

        assertThat(status).isEqualTo("READY");
    }

    @Test
    @DisplayName("주문 ID로 결제를 찾을 수 있고, 없는 주문이면 빈 결과가 나온다")
    void findsByOrderId() {
        paymentRepository.save(Payment.ready(101L, 1L, 30_000L));
        entityManager.flush();
        entityManager.clear();

        Optional<Payment> found = paymentRepository.findByOrderId(101L);
        Optional<Payment> notFound = paymentRepository.findByOrderId(999L);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(30_000L);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("같은 주문으로 결제를 두 번 저장하면 유니크 제약이 막는다")
    void rejectsDuplicateOrderId() {
        paymentRepository.save(Payment.ready(101L, 1L, 30_000L));
        entityManager.flush();

        assertThatThrownBy(() -> paymentRepository.save(Payment.ready(101L, 2L, 30_000L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}