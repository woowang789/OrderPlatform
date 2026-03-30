package com.orderplatform.product.adapter.out.persistence;

import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;

/**
 * Domain Product ↔ ProductJpaEntity 양방향 변환
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    public static ProductJpaEntity toJpaEntity(Product product) {
        return new ProductJpaEntity(
                product.getName(),
                product.getPrice().amount(),
                product.getStock().quantity(),
                product.getStock().threshold(),
                product.getCategory()
        );
    }

    public static Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                entity.getId(),
                entity.getName(),
                new Money(entity.getPrice()),
                new Stock(entity.getStock(), entity.getStockThreshold()),
                entity.getCategory(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
