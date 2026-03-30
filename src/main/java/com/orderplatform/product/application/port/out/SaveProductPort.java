package com.orderplatform.product.application.port.out;

import com.orderplatform.product.domain.model.Product;

public interface SaveProductPort {

    Product save(Product product);
}
