package com.orderplatform.order.adapter;

import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.out.persistence.MemberPersistenceAdapter;
import com.orderplatform.member.domain.model.Email;
import com.orderplatform.member.domain.model.Member;
import com.orderplatform.order.adapter.out.persistence.OrderPersistenceAdapter;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired OrderPersistenceAdapter adapter;
    @Autowired MemberPersistenceAdapter memberAdapter;

    private Long memberId;

    @BeforeEach
    void setUp() {
        Member member = memberAdapter.save(
                Member.create(new Email("order-" + UUID.randomUUID() + "@test.com"), "pw", "주문테스트")
        );
        memberId = member.getId();
    }

    @Test
    void save_후_findById_OrderLine_포함() {
        List<OrderLine> lines = List.of(
                new OrderLine(1L, "상품A", 10000, 2),
                new OrderLine(2L, "상품B", 5000, 1)
        );
        Order order = Order.create(memberId, lines);

        Order saved = adapter.save(order);
        Optional<Order> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrderLines()).hasSize(2);
        assertThat(found.get().getMemberId()).isEqualTo(memberId);
    }

    @Test
    void findAllByMemberId() {
        adapter.save(Order.create(memberId, List.of(new OrderLine(1L, "A", 1000, 1))));
        adapter.save(Order.create(memberId, List.of(new OrderLine(2L, "B", 2000, 1))));

        List<Order> orders = adapter.findAllByMemberId(memberId);

        assertThat(orders.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void OrderLine_매핑_정합성() {
        List<OrderLine> lines = List.of(
                new OrderLine(99L, "정합성테스트상품", 15000, 3)
        );
        Order order = Order.create(memberId, lines);

        Order saved = adapter.save(order);
        Order found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getOrderLines()).hasSize(1);
        OrderLine line = found.getOrderLines().get(0);
        assertThat(line.productId()).isEqualTo(99L);
        assertThat(line.productName()).isEqualTo("정합성테스트상품");
        assertThat(line.price()).isEqualTo(15000);
        assertThat(line.quantity()).isEqualTo(3);
    }
}
