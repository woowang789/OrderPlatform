package com.orderplatform.product.domain;

import com.orderplatform.product.domain.exception.InsufficientStockException;
import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Product 도메인 모델 단위 테스트 — 순수 Java (Spring 없음)
 */
class ProductDomainTest {

    @Test
    void create_정적팩토리() {
        Product product = Product.create("테스트상품", new Money(10000), new Stock(100, 10), "음식");

        assertThat(product.getId()).isNull();
        assertThat(product.getName()).isEqualTo("테스트상품");
        assertThat(product.getPrice().amount()).isEqualTo(10000);
        assertThat(product.getStock().quantity()).isEqualTo(100);
        assertThat(product.getCategory()).isEqualTo("음식");
        assertThat(product.getCreatedAt()).isNull();
        assertThat(product.getUpdatedAt()).isNull();
    }

    @Test
    void decreaseStock_정상_차감() {
        Product product = Product.create("상품", new Money(1000), new Stock(10, 0), "카테고리");

        product.decreaseStock(3);

        assertThat(product.getStock().quantity()).isEqualTo(7);
    }

    @Test
    void decreaseStock_재고_부족시_예외() {
        Product product = Product.create("상품", new Money(1000), new Stock(5, 0), "카테고리");

        assertThatThrownBy(() -> product.decreaseStock(10))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void restoreStock_정상_복원() {
        Product product = Product.create("상품", new Money(1000), new Stock(5, 0), "카테고리");

        product.restoreStock(3);

        assertThat(product.getStock().quantity()).isEqualTo(8);
    }

    @Test
    void reconstitute_모든_필드_복원() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Product product = Product.reconstitute(
                1L, "상품", new Money(5000), new Stock(50, 10), "카테고리", now, now
        );

        assertThat(product.getId()).isEqualTo(1L);
        assertThat(product.getName()).isEqualTo("상품");
        assertThat(product.getPrice().amount()).isEqualTo(5000);
        assertThat(product.getStock().quantity()).isEqualTo(50);
        assertThat(product.getStock().threshold()).isEqualTo(10);
    }
}
