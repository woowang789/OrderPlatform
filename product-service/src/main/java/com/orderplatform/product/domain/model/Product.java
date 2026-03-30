package com.orderplatform.product.domain.model;

import com.orderplatform.common.domain.model.Money;

import java.time.LocalDateTime;

/**
 * Product 도메인 모델 — 순수 Java 객체 (JPA/Spring 의존 없음)
 */
public class Product {

    private final Long id;
    private final String name;
    private final Money price;
    private Stock stock;
    private final String category;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Product(Long id, String name, Money price, Stock stock, String category,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 신규 상품 생성 — id와 timestamp는 영속화 시 설정
     */
    public static Product create(String name, Money price, Stock stock, String category) {
        return new Product(null, name, price, stock, category, null, null);
    }

    /**
     * DB에서 복원 — 모든 필드를 포함한 완전한 상태 복원
     */
    public static Product reconstitute(Long id, String name, Money price, Stock stock,
                                       String category, LocalDateTime createdAt,
                                       LocalDateTime updatedAt) {
        return new Product(id, name, price, stock, category, createdAt, updatedAt);
    }

    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);
    }

    public void restoreStock(int quantity) {
        this.stock = this.stock.increase(quantity);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Money getPrice() {
        return price;
    }

    public Stock getStock() {
        return stock;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
