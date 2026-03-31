package com.orderplatform.product.application.service;

import com.orderplatform.common.domain.event.StockDeductedEvent;
import com.orderplatform.common.domain.event.payload.StockItemPayload;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand.DeductItem;
import com.orderplatform.product.application.port.in.DeductStockForOrderUseCase;
import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.port.out.ProductEventPublishPort;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.domain.exception.InsufficientStockException;
import com.orderplatform.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 주문 단위 원자적 재고 차감 서비스
 * 멀티 아이템을 단일 트랜잭션으로 처리하며, 하나라도 실패 시 전체 롤백한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeductStockForOrderService implements DeductStockForOrderUseCase {

    private final LoadProductPort loadProductPort;
    private final SaveProductPort saveProductPort;
    private final ProductEventPublishPort productEventPublishPort;

    @Override
    @Transactional
    public void deductStockForOrder(DeductStockForOrderCommand command) {
        log.info("주문 단위 재고 차감 시작 — orderId: {}, items: {}", command.orderId(), command.items().size());

        // productId 순으로 정렬하여 데드락 방지
        List<DeductItem> sortedItems = command.items().stream()
                .sorted(Comparator.comparing(DeductItem::productId))
                .toList();

        try {
            List<StockItemPayload> deductedItems = new ArrayList<>();

            for (DeductItem item : sortedItems) {
                Product product = loadProductPort.findByIdForUpdate(item.productId())
                        .orElseThrow(() -> new IllegalStateException(
                                "상품을 찾을 수 없습니다. productId: " + item.productId()));

                product.decreaseStock(item.quantity());
                saveProductPort.save(product);

                deductedItems.add(new StockItemPayload(item.productId(), item.quantity()));
            }

            // 전체 성공 → StockDeductedEvent 발행
            productEventPublishPort.publishStockDeducted(
                    new StockDeductedEvent(command.orderId(), deductedItems));

            log.info("주문 단위 재고 차감 완료 — orderId: {}", command.orderId());

        } catch (InsufficientStockException e) {
            log.warn("재고 부족으로 차감 실패 — orderId: {}, reason: {}", command.orderId(), e.getMessage());

            // 트랜잭션 롤백을 위해 예외를 던지되, 이벤트 발행은 Consumer에서 처리
            throw e;
        }
    }
}
