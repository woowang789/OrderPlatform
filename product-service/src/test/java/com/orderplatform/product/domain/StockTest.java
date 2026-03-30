package com.orderplatform.product.domain;

import com.orderplatform.product.domain.exception.InsufficientStockException;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Stock Value Object 단위 테스트 — 순수 Java (Spring 없음)
 */
class StockTest {

    @Test
    void 수량_음수면_예외() {
        assertThatThrownBy(() -> new Stock(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 임계값_음수면_예외() {
        assertThatThrownBy(() -> new Stock(10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrease_정상_차감() {
        Stock stock = new Stock(10, 5);

        Stock decreased = stock.decrease(3);

        assertThat(decreased.quantity()).isEqualTo(7);
        assertThat(decreased.threshold()).isEqualTo(5);
    }

    @Test
    void decrease_재고_부족시_InsufficientStockException() {
        Stock stock = new Stock(5, 0);

        assertThatThrownBy(() -> stock.decrease(10))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void decrease_음수_요청시_예외() {
        Stock stock = new Stock(10, 0);

        assertThatThrownBy(() -> stock.decrease(-1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stock.decrease(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void increase_정상_복원() {
        Stock stock = new Stock(5, 0);

        Stock increased = stock.increase(3);

        assertThat(increased.quantity()).isEqualTo(8);
    }

    @Test
    void increase_음수_요청시_예외() {
        Stock stock = new Stock(10, 0);

        assertThatThrownBy(() -> stock.increase(-1))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> stock.increase(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void isLowStock_경계값() {
        // 수량 == 임계값 → lowStock
        assertThat(new Stock(5, 5).isLowStock()).isTrue();

        // 수량 < 임계값 → lowStock
        assertThat(new Stock(3, 5).isLowStock()).isTrue();

        // 수량 > 임계값 → not lowStock
        assertThat(new Stock(10, 5).isLowStock()).isFalse();
    }

    @Test
    void decrease_후_불변성_보장() {
        Stock original = new Stock(10, 5);

        Stock decreased = original.decrease(3);

        assertThat(original.quantity()).isEqualTo(10);
        assertThat(decreased.quantity()).isEqualTo(7);
    }
}
