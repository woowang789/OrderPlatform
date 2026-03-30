package com.orderplatform.product.application.port.in;

public interface CreateProductUseCase {

    ProductInfo createProduct(CreateProductCommand command);
}
