package com.orderplatform.product.domain.model;

/**
 * 금액 Value Object — 원 단위 (불변)
 */
public record Money(long amount) {

    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0) {
            throw new IllegalArgumentException("금액은 음수일 수 없습니다. 입력값: " + amount);
        }
    }

    public Money multiply(int quantity) {
        return new Money(this.amount * quantity);
    }
}
