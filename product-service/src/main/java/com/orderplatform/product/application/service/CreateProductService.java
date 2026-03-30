package com.orderplatform.product.application.service;

import com.orderplatform.product.application.port.in.CreateProductCommand;
import com.orderplatform.product.application.port.in.CreateProductUseCase;
import com.orderplatform.product.application.port.in.ProductInfo;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CreateProductService implements CreateProductUseCase {

    private final SaveProductPort saveProductPort;

    @Override
    public ProductInfo createProduct(CreateProductCommand command) {
        Product product = Product.create(
                command.name(),
                new Money(command.price()),
                new Stock(command.stock(), 0),
                command.category()
        );
        Product saved = saveProductPort.save(product);

        return new ProductInfo(
                saved.getId(),
                saved.getName(),
                saved.getPrice().amount(),
                saved.getStock().quantity(),
                saved.getCategory(),
                saved.getCreatedAt()
        );
    }
}
