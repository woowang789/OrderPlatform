package com.orderplatform.product.adapter.out.persistence;

import com.orderplatform.product.application.port.out.LoadProductPort;
import com.orderplatform.product.application.port.out.SaveProductPort;
import com.orderplatform.product.domain.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Component
@RequiredArgsConstructor
public class ProductPersistenceAdapter implements LoadProductPort, SaveProductPort {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id)
                .map(ProductMapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return productJpaRepository.findAll().stream()
                .map(ProductMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return productJpaRepository.findByIdForUpdate(id)
                .map(ProductMapper::toDomain);
    }

    @Override
    public Product save(Product product) {
        if (product.getId() == null) {
            // 신규 생성
            ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
            ProductJpaEntity saved = productJpaRepository.save(entity);
            return ProductMapper.toDomain(saved);
        } else {
            // 업데이트 — 영속 상태의 엔티티를 조회하여 필드 갱신 (dirty checking)
            ProductJpaEntity entity = productJpaRepository.findById(product.getId())
                    .orElseThrow();
            entity.updateFrom(
                    product.getName(),
                    product.getPrice().amount(),
                    product.getStock().quantity(),
                    product.getStock().threshold(),
                    product.getCategory()
            );
            return ProductMapper.toDomain(entity);
        }
    }
}
