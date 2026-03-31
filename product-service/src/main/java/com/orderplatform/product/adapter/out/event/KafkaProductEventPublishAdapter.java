package com.orderplatform.product.adapter.out.event;

import com.orderplatform.common.domain.event.StockDeductedEvent;
import com.orderplatform.common.domain.event.StockDeductionFailedEvent;
import com.orderplatform.common.domain.event.Topics;
import com.orderplatform.product.application.port.out.ProductEventPublishPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka 기반 상품 이벤트 발행 어댑터
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProductEventPublishAdapter implements ProductEventPublishPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishStockDeducted(StockDeductedEvent event) {
        log.info("재고 차감 완료 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.STOCK_DEDUCTED, event.orderId().toString(), event);
    }

    @Override
    public void publishStockDeductionFailed(StockDeductionFailedEvent event) {
        log.info("재고 차감 실패 이벤트 발행 — orderId: {}", event.orderId());
        kafkaTemplate.send(Topics.STOCK_DEDUCTION_FAILED, event.orderId().toString(), event);
    }
}
