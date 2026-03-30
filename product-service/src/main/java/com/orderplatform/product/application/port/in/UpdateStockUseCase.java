package com.orderplatform.product.application.port.in;

public interface UpdateStockUseCase {

    void decreaseStock(Long productId, int quantity);

    void decreaseStockWithOptimisticLock(Long productId, int quantity);
}
