package com.orderplatform.product.application;

import com.orderplatform.product.application.port.in.CreateProductCommand;
import com.orderplatform.product.application.port.in.ProductInfo;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.application.service.CreateProductService;
import com.orderplatform.product.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateProductServiceTest {

    @Mock
    SaveProductPort saveProductPort;
    @InjectMocks
    CreateProductService createProductService;

    @Test
    void 정상_상품_생성() {
        CreateProductCommand command = new CreateProductCommand("테스트상품", 10000, 100, "음식");
        LocalDateTime now = LocalDateTime.now();

        given(saveProductPort.save(any(Product.class))).willReturn(
                Product.reconstitute(1L, "테스트상품", new Money(10000), new Stock(100, 0), "음식", now, now)
        );

        ProductInfo result = createProductService.createProduct(command);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("테스트상품");
        assertThat(result.price()).isEqualTo(10000);
        assertThat(result.stock()).isEqualTo(100);
        verify(saveProductPort).save(any(Product.class));
    }
}
