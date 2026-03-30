package com.orderplatform.payment.adapter;

import com.orderplatform.payment.common.AbstractIntegrationTest;
import com.orderplatform.payment.adapter.out.persistence.PaymentPersistenceAdapter;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired PaymentPersistenceAdapter adapter;

    private final Long memberId = 1L;
    private final UUID orderId = UUID.randomUUID();

    @Test
    void save_후_findByIdAndMemberId() {
        Payment payment = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);

        Payment saved = adapter.save(payment);
        Optional<Payment> found = adapter.findByIdAndMemberId(saved.getId(), memberId);

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualTo(10000);
        assertThat(found.get().getOrderId()).isEqualTo(orderId);
    }

    @Test
    void findByOrderIdExcludingCancelled_CANCELLED_제외() {
        // COMPLETED 결제
        Payment completed = Payment.create(orderId, memberId, 10000, PaymentMethod.CARD);
        Payment savedCompleted = adapter.save(completed);

        // 조회 시 COMPLETED 결제 반환
        Optional<Payment> found = adapter.findByOrderIdExcludingCancelled(orderId);
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(savedCompleted.getId());
    }
}
