package com.orderplatform.product.application.service;

import com.orderplatform.product.application.port.in.UpdateStockUseCase;
import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.domain.exception.ProductNotFoundException;
import com.orderplatform.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStockService implements UpdateStockUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;

    @Override
    public void decreaseStock(Long productId, int quantity) {
        Product product = loadProductPort.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.decreaseStock(quantity);
        saveProductPort.save(product);
    }

    /**
     * 낙관적 락 기반 재고 차감 (비관적 락과 성능 비교용)
     * - findById()로 조회 (비관적 락 없음)
     * - @Version에 의해 커밋 시 버전 충돌 감지
     */
    @Override
    public void decreaseStockWithOptimisticLock(Long productId, int quantity) {
        Product product = loadProductPort.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.decreaseStock(quantity);
        saveProductPort.save(product);
    }
}
