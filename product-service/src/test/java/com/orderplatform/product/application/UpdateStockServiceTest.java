package com.orderplatform.product.application;

import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.application.service.UpdateStockService;
import com.orderplatform.product.domain.exception.ProductNotFoundException;
import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UpdateStockServiceTest {

    @Mock
    LoadProductPort loadProductPort;
    @Mock
    SaveProductPort saveProductPort;
    @InjectMocks
    UpdateStockService updateStockService;

    private final LocalDateTime now = LocalDateTime.now();

    private Product sampleProduct() {
        return Product.reconstitute(1L, "상품", new Money(10000), new Stock(100, 10), "음식", now, now);
    }

    @Test
    void 비관적_락_재고_차감() {
        Product product = sampleProduct();
        given(loadProductPort.findByIdForUpdate(1L)).willReturn(Optional.of(product));

        updateStockService.decreaseStock(1L, 5);

        verify(saveProductPort).save(any(Product.class));
    }

    @Test
    void 낙관적_락_재고_차감() {
        Product product = sampleProduct();
        given(loadProductPort.findById(1L)).willReturn(Optional.of(product));

        updateStockService.decreaseStockWithOptimisticLock(1L, 5);

        verify(saveProductPort).save(any(Product.class));
    }

    @Test
    void 비관적_락_미존재시_예외() {
        given(loadProductPort.findByIdForUpdate(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> updateStockService.decreaseStock(999L, 5))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 낙관적_락_미존재시_예외() {
        given(loadProductPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> updateStockService.decreaseStockWithOptimisticLock(999L, 5))
                .isInstanceOf(ProductNotFoundException.class);
    }
}
