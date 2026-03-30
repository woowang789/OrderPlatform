package com.orderplatform.product.application.port.in;

import java.util.List;

public interface GetProductUseCase {

    ProductInfo getProduct(Long id);

    List<ProductInfo> getProducts();
}
