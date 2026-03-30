package com.orderplatform.product.domain;

import com.orderplatform.product.domain.model.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Money Value Object 단위 테스트 — 순수 Java (Spring 없음)
 */
class MoneyTest {

    @Test
    void 금액은_음수면_예외() {
        assertThatThrownBy(() -> new Money(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 금액_0은_허용() {
        Money money = new Money(0);
        assertThat(money.amount()).isZero();
    }

    @Test
    void ZERO_상수_검증() {
        assertThat(Money.ZERO.amount()).isZero();
    }

    @Test
    void multiply_연산_검증() {
        Money money = new Money(1000);

        Money result = money.multiply(3);

        assertThat(result.amount()).isEqualTo(3000);
    }

    @Test
    void multiply_0배_검증() {
        Money money = new Money(1000);

        Money result = money.multiply(0);

        assertThat(result.amount()).isZero();
    }

    @Test
    void 동일한_금액은_동등하다() {
        Money money1 = new Money(5000);
        Money money2 = new Money(5000);

        assertThat(money1).isEqualTo(money2);
    }
}
