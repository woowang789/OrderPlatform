package com.orderplatform.product.entity;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.common.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private int stock;

    private String category;

    @Version
    private Long version;

    public Product(String name, long price, int stock, String category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 양수여야 합니다. 요청 수량: " + quantity);
        }
        if (this.stock < quantity) {
            throw new InsufficientStockException(this.stock, quantity);
        }
        this.stock -= quantity;
    }

    /**
     * 주문 취소 시 재고 복원
     */
    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복원 수량은 양수여야 합니다. 요청 수량: " + quantity);
        }
        this.stock += quantity;
    }
}
