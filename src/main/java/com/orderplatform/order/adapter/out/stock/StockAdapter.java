package com.orderplatform.order.adapter.out.stock;

import com.orderplatform.order.application.port.out.DecreaseStockPort;
import com.orderplatform.order.application.port.out.RestoreStockPort;
import com.orderplatform.order.application.port.out.StockInfo;
import com.orderplatform.product.adapter.out.persistence.ProductJpaEntity;
import com.orderplatform.product.adapter.out.persistence.ProductJpaRepository;
import com.orderplatform.product.domain.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 재고 차감/복원 어댑터 — Product 컨텍스트의 JPA 인프라 직접 사용 (모놀리스)
 * Phase 3 MSA 전환 시 Kafka 이벤트 기반으로 교체 예정
 */
@Component
@RequiredArgsConstructor
public class StockAdapter implements DecreaseStockPort, RestoreStockPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public StockInfo decreaseStock(Long productId, int quantity) {
        ProductJpaEntity product = productJpaRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.decreaseStock(quantity);

        return new StockInfo(product.getName(), product.getPrice());
    }

    @Override
    public void restoreStock(Long productId, int quantity) {
        ProductJpaEntity product = productJpaRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        product.restoreStock(quantity);
    }
}
