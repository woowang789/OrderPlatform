package com.orderplatform.order.domain;

import com.orderplatform.order.domain.exception.InvalidOrderStatusException;
import com.orderplatform.order.domain.model.Order;
import com.orderplatform.order.domain.model.OrderLine;
import com.orderplatform.order.domain.model.OrderStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Order 도메인 모델 단위 테스트 — 순수 Java (Spring 없음)
 */
class OrderDomainTest {

    private List<OrderLine> sampleOrderLines() {
        return List.of(
                new OrderLine(1L, "상품A", 10000, 2),
                new OrderLine(2L, "상품B", 5000, 1)
        );
    }

    @Test
    void create_빈_orderLines면_예외() {
        assertThatThrownBy(() -> Order.create(1L, Collections.emptyList()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_null_orderLines면_예외() {
        assertThatThrownBy(() -> Order.create(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_totalAmount_자동_계산() {
        Order order = Order.create(1L, sampleOrderLines());

        // 10000*2 + 5000*1 = 25000
        assertThat(order.getTotalAmount()).isEqualTo(25000);
    }

    @Test
    void create_후_상태는_PLACED() {
        Order order = Order.create(1L, sampleOrderLines());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
    }

    @Test
    void create_후_id는_null() {
        Order order = Order.create(1L, sampleOrderLines());

        assertThat(order.getId()).isNull();
    }

    @Test
    void markPaid_PLACED에서_PAID() {
        Order order = Order.create(1L, sampleOrderLines());

        order.markPaid();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void markPaid_비PLACED면_예외() {
        Order order = Order.create(1L, sampleOrderLines());
        order.markPaid(); // PAID 상태

        assertThatThrownBy(() -> order.markPaid())
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void cancel_PLACED에서_CANCELLED() {
        Order order = Order.create(1L, sampleOrderLines());

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_PAID에서_CANCELLED() {
        Order order = Order.create(1L, sampleOrderLines());
        order.markPaid();

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancel_CANCELLED에서_예외() {
        Order order = Order.create(1L, sampleOrderLines());
        order.cancel();

        assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(InvalidOrderStatusException.class);
    }

    @Test
    void getOrderLines_불변_리스트() {
        Order order = Order.create(1L, sampleOrderLines());

        List<OrderLine> lines = order.getOrderLines();

        assertThatThrownBy(() -> lines.add(new OrderLine(3L, "상품C", 3000, 1)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void reconstitute_모든_필드_복원() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<OrderLine> lines = sampleOrderLines();

        Order order = Order.reconstitute(
                id, 1L, OrderStatus.PAID, 25000, 1L, lines, now, now
        );

        assertThat(order.getId()).isEqualTo(id);
        assertThat(order.getMemberId()).isEqualTo(1L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getTotalAmount()).isEqualTo(25000);
        assertThat(order.getVersion()).isEqualTo(1L);
        assertThat(order.getOrderLines()).hasSize(2);
    }
}
