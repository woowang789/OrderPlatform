package com.orderplatform.product.application.port.out;

import com.orderplatform.common.domain.event.StockDeductedEvent;
import com.orderplatform.common.domain.event.StockDeductionFailedEvent;

/**
 * 상품 이벤트 발행 Outbound Port
 */
public interface ProductEventPublishPort {

    void publishStockDeducted(StockDeductedEvent event);

    void publishStockDeductionFailed(StockDeductionFailedEvent event);
}
