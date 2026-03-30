package com.orderplatform.order.domain;

import com.orderplatform.order.domain.model.OrderLine;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OrderLine Value Object 단위 테스트 — 순수 Java (Spring 없음)
 */
class OrderLineTest {

    @Test
    void 동일_값_동등성() {
        OrderLine line1 = new OrderLine(1L, "상품A", 10000, 2);
        OrderLine line2 = new OrderLine(1L, "상품A", 10000, 2);

        assertThat(line1).isEqualTo(line2);
        assertThat(line1.hashCode()).isEqualTo(line2.hashCode());
    }

    @Test
    void 다른_값_비동등성() {
        OrderLine line1 = new OrderLine(1L, "상품A", 10000, 2);
        OrderLine line2 = new OrderLine(2L, "상품B", 20000, 1);

        assertThat(line1).isNotEqualTo(line2);
    }

    @Test
    void 필드_접근_검증() {
        OrderLine line = new OrderLine(1L, "테스트상품", 15000, 3);

        assertThat(line.productId()).isEqualTo(1L);
        assertThat(line.productName()).isEqualTo("테스트상품");
        assertThat(line.price()).isEqualTo(15000);
        assertThat(line.quantity()).isEqualTo(3);
    }
}
