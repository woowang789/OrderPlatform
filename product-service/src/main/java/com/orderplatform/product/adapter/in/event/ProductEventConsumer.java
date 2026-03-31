package com.orderplatform.product.adapter.in.event;

import com.orderplatform.common.domain.event.PaymentCompletedEvent;
import com.orderplatform.common.domain.event.StockDeductionFailedEvent;
import com.orderplatform.common.domain.event.Topics;
import com.orderplatform.common.domain.event.payload.StockFailureItemPayload;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand.DeductItem;
import com.orderplatform.product.application.port.in.DeductStockForOrderUseCase;
import com.orderplatform.product.application.port.out.ProductEventPublishPort;
import com.orderplatform.product.domain.exception.InsufficientStockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 이벤트 소비 어댑터 — 재고 차감
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final DeductStockForOrderUseCase deductStockForOrderUseCase;
    private final ProductEventPublishPort productEventPublishPort;

    /**
     * 결제 완료 이벤트 소비 → 주문 단위 원자적 재고 차감
     */
    @KafkaListener(topics = Topics.PAYMENT_COMPLETED, groupId = "product-group")
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("결제 완료 이벤트 수신 — orderId: {}", event.orderId());

        List<DeductItem> items = event.items().stream()
                .map(item -> new DeductItem(item.productId(), item.quantity()))
                .toList();

        DeductStockForOrderCommand command = new DeductStockForOrderCommand(event.orderId(), items);

        try {
            deductStockForOrderUseCase.deductStockForOrder(command);
        } catch (InsufficientStockException e) {
            log.warn("재고 부족으로 차감 실패 — orderId: {}", event.orderId());

            List<StockFailureItemPayload> failedItems = event.items().stream()
                    .map(item -> new StockFailureItemPayload(item.productId(), item.quantity(), 0))
                    .toList();

            productEventPublishPort.publishStockDeductionFailed(
                    new StockDeductionFailedEvent(event.orderId(), failedItems, e.getMessage()));
        } catch (Exception e) {
            log.error("재고 차감 중 예상치 못한 오류 — orderId: {}", event.orderId(), e);

            productEventPublishPort.publishStockDeductionFailed(
                    new StockDeductionFailedEvent(event.orderId(), List.of(), e.getMessage()));
        }
    }
}
