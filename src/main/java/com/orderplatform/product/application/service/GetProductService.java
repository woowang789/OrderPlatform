package com.orderplatform.product.application.service;

import com.orderplatform.product.application.port.in.GetProductUseCase;
import com.orderplatform.product.application.port.in.ProductInfo;
import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.domain.exception.ProductNotFoundException;
import com.orderplatform.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetProductService implements GetProductUseCase {

    private final LoadProductPort loadProductPort;

    @Override
    public ProductInfo getProduct(Long id) {
        Product product = loadProductPort.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toProductInfo(product);
    }

    @Override
    public List<ProductInfo> getProducts() {
        return loadProductPort.findAll().stream()
                .map(this::toProductInfo)
                .toList();
    }

    private ProductInfo toProductInfo(Product product) {
        return new ProductInfo(
                product.getId(),
                product.getName(),
                product.getPrice().amount(),
                product.getStock().quantity(),
                product.getCategory(),
                product.getCreatedAt()
        );
    }
}
