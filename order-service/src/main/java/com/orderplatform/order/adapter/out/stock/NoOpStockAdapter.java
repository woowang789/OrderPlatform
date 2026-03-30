package com.orderplatform.order.adapter.out.stock;

import com.orderplatform.order.application.port.out.DecreaseStockPort;
import com.orderplatform.order.application.port.out.RestoreStockPort;
import com.orderplatform.order.application.port.out.StockInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 재고 차감/복원 Stub — Phase 3 6단계에서 Kafka 이벤트 기반으로 전환 예정
 */
@Slf4j
@Component
public class NoOpStockAdapter implements DecreaseStockPort, RestoreStockPort {

    @Override
    public StockInfo decreaseStock(Long productId, int quantity) {
        log.info("[Stub] 재고 차감 — productId: {}, quantity: {} (Phase 3 6단계에서 이벤트로 전환 예정)",
                productId, quantity);
        return new StockInfo("stub-product", 10000);
    }

    @Override
    public void restoreStock(Long productId, int quantity) {
        log.info("[Stub] 재고 복원 — productId: {}, quantity: {} (Phase 3 6단계에서 이벤트로 전환 예정)",
                productId, quantity);
    }
}
