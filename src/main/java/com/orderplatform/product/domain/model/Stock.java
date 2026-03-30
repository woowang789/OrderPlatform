package com.orderplatform.product.domain.model;

import com.orderplatform.product.domain.exception.InsufficientStockException;

/**
 * 재고 Value Object — 수량 + 임계값 (불변)
 */
public record Stock(int quantity, int threshold) {

    public Stock {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고 수량은 음수일 수 없습니다. 입력값: " + quantity);
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("재고 임계값은 음수일 수 없습니다. 입력값: " + threshold);
        }
    }

    public Stock decrease(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("차감 수량은 양수여야 합니다. 요청 수량: " + qty);
        }
        if (this.quantity < qty) {
            throw new InsufficientStockException(this.quantity, qty);
        }
        return new Stock(this.quantity - qty, this.threshold);
    }

    public Stock increase(int qty) {
        if (qty <= 0) {
            throw new IllegalArgumentException("복원 수량은 양수여야 합니다. 요청 수량: " + qty);
        }
        return new Stock(this.quantity + qty, this.threshold);
    }

    public boolean isLowStock() {
        return this.quantity <= this.threshold;
    }
}
