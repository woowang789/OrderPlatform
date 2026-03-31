package com.orderplatform.product.application;

import com.orderplatform.common.domain.event.StockDeductedEvent;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand;
import com.orderplatform.product.application.port.in.DeductStockForOrderCommand.DeductItem;
import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.port.out.ProductEventPublishPort;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.application.service.DeductStockForOrderService;
import com.orderplatform.product.domain.exception.InsufficientStockException;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeductStockForOrderServiceTest {

    @Mock LoadProductPort loadProductPort;
    @Mock SaveProductPort saveProductPort;
    @Mock ProductEventPublishPort productEventPublishPort;
    @InjectMocks DeductStockForOrderService deductStockForOrderService;

    private final UUID orderId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.now();

    private Product createProduct(Long id, int stockQty) {
        return Product.reconstitute(id, "상품" + id, new Money(10000),
                new Stock(stockQty, 5), "카테고리", now, now);
    }

    @Test
    void 정상_단일_아이템_재고_차감() {
        List<DeductItem> items = List.of(new DeductItem(1L, 3));
        DeductStockForOrderCommand command = new DeductStockForOrderCommand(orderId, items);

        given(loadProductPort.findByIdForUpdate(1L))
                .willReturn(Optional.of(createProduct(1L, 10)));
        given(saveProductPort.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        deductStockForOrderService.deductStockForOrder(command);

        verify(saveProductPort).save(any(Product.class));
        verify(productEventPublishPort).publishStockDeducted(any(StockDeductedEvent.class));
    }

    @Test
    void 정상_멀티_아이템_원자적_차감() {
        List<DeductItem> items = List.of(
                new DeductItem(1L, 2),
                new DeductItem(2L, 3)
        );
        DeductStockForOrderCommand command = new DeductStockForOrderCommand(orderId, items);

        given(loadProductPort.findByIdForUpdate(1L))
                .willReturn(Optional.of(createProduct(1L, 10)));
        given(loadProductPort.findByIdForUpdate(2L))
                .willReturn(Optional.of(createProduct(2L, 10)));
        given(saveProductPort.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        deductStockForOrderService.deductStockForOrder(command);

        verify(productEventPublishPort).publishStockDeducted(any(StockDeductedEvent.class));
    }

    @Test
    void 재고_부족시_예외_발생() {
        List<DeductItem> items = List.of(new DeductItem(1L, 100));
        DeductStockForOrderCommand command = new DeductStockForOrderCommand(orderId, items);

        given(loadProductPort.findByIdForUpdate(1L))
                .willReturn(Optional.of(createProduct(1L, 5)));

        assertThatThrownBy(() -> deductStockForOrderService.deductStockForOrder(command))
                .isInstanceOf(InsufficientStockException.class);

        verify(productEventPublishPort, never()).publishStockDeducted(any());
    }

    @Test
    void 상품_미존재시_예외_발생() {
        List<DeductItem> items = List.of(new DeductItem(999L, 1));
        DeductStockForOrderCommand command = new DeductStockForOrderCommand(orderId, items);

        given(loadProductPort.findByIdForUpdate(999L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> deductStockForOrderService.deductStockForOrder(command))
                .isInstanceOf(IllegalStateException.class);
    }
}
