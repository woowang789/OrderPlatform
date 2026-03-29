package com.orderplatform.product.service;

import com.orderplatform.common.exception.ProductNotFoundException;
import com.orderplatform.product.dto.CreateProductRequest;
import com.orderplatform.product.dto.ProductResponse;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = new Product(
                request.name(),
                request.price(),
                request.stock(),
                request.category()
        );
        Product saved = productRepository.save(product);
        return ProductResponse.from(saved);
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public ProductResponse getProduct(@NonNull Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductResponse.from(product);
    }

    @Transactional
    public void decreaseStock(@NonNull Long productId, int quantity) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.decreaseStock(quantity);
    }

    /**
     * 낙관적 락 기반 재고 차감 (비관적 락과 성능 비교용)
     * - findById()로 조회 (비관적 락 없음)
     * - @Version에 의해 커밋 시 버전 충돌 감지
     */
    @Transactional
    public void decreaseStockWithOptimisticLock(@NonNull Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        product.decreaseStock(quantity);
    }
}
