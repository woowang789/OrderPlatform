package com.orderplatform.product.adapter.out.persistence;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.product.domain.exception.InsufficientStockException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false)
    private int stockThreshold;

    private String category;

    @Version
    private Long version;

    public ProductJpaEntity(String name, long price, int stock, int stockThreshold, String category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.stockThreshold = stockThreshold;
        this.category = category;
    }

    /**
     * 도메인 모델 변경사항을 JPA 엔티티에 반영 (Adapter save 시 사용)
     */
    void updateFrom(String name, long price, int stock, int stockThreshold, String category) {
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.stockThreshold = stockThreshold;
        this.category = category;
    }

    // === 임시 메서드 — OrderService 전환(5단계) 전까지 사용 ===

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 양수여야 합니다. 요청 수량: " + quantity);
        }
        if (this.stock < quantity) {
            throw new InsufficientStockException(this.stock, quantity);
        }
        this.stock -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("복원 수량은 양수여야 합니다. 요청 수량: " + quantity);
        }
        this.stock += quantity;
    }
}
