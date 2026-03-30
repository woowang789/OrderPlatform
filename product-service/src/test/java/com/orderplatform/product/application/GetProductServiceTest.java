package com.orderplatform.product.application;

import com.orderplatform.product.application.port.in.ProductInfo;
import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.service.GetProductService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class GetProductServiceTest {

    @Mock
    LoadProductPort loadProductPort;
    @InjectMocks
    GetProductService getProductService;

    private final LocalDateTime now = LocalDateTime.now();

    private Product sampleProduct(Long id, String name) {
        return Product.reconstitute(id, name, new Money(10000), new Stock(50, 5), "음식", now, now);
    }

    @Test
    void 단건_조회() {
        given(loadProductPort.findById(1L)).willReturn(Optional.of(sampleProduct(1L, "상품A")));

        ProductInfo result = getProductService.getProduct(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("상품A");
    }

    @Test
    void 미존재시_예외() {
        given(loadProductPort.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> getProductService.getProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void 전체_조회() {
        given(loadProductPort.findAll()).willReturn(
                List.of(sampleProduct(1L, "상품A"), sampleProduct(2L, "상품B"))
        );

        List<ProductInfo> result = getProductService.getProducts();

        assertThat(result).hasSize(2);
    }
}
