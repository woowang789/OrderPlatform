package com.orderplatform.product.application.port.out;

import com.orderplatform.product.domain.model.Product;

import java.util.List;
import java.util.Optional;

public interface LoadProductPort {

    Optional<Product> findById(Long id);

    List<Product> findAll();

    Optional<Product> findByIdForUpdate(Long id);
}
