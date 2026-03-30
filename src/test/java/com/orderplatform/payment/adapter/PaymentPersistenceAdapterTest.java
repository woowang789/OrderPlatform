package com.orderplatform.payment.adapter;

import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.out.persistence.MemberPersistenceAdapter;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import com.orderplatform.order.adapter.out.persistence.OrderPersistenceAdapter;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import com.orderplatform.payment.adapter.out.persistence.PaymentPersistenceAdapter;
import com.orderplatform.payment.domain.model.Payment;
import com.orderplatform.payment.domain.model.PaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired PaymentPersistenceAdapter adapter;
    @Autowired MemberPersistenceAdapter memberAdapter;
    @Autowired OrderPersistenceAdapter orderAdapter;

    private Long memberId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        // 선행 데이터: 회원 + 주문
        Member member = memberAdapter.save(
                Member.create(new Email("pay-" + UUID.randomUUID() + "@test.com"), "pw", "결제테스트")
        );
        memberId = member.getId();

        Order order = orderAdapter.save(
                Order.create(memberId, List.of(new OrderLine(1L, "상품", 10000, 1)))
        );
        orderId = order.getId();
    }

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
