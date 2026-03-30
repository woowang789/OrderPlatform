package com.orderplatform.product.adapter;

import com.orderplatform.product.common.AbstractIntegrationTest;
import com.orderplatform.product.adapter.out.persistence.ProductPersistenceAdapter;
import com.orderplatform.common.domain.model.Money;
import com.orderplatform.product.domain.model.Product;
import com.orderplatform.product.domain.model.Stock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ProductPersistenceAdapterTest extends AbstractIntegrationTest {

    @Autowired
    ProductPersistenceAdapter adapter;

    @Test
    void save_후_findById() {
        Product product = Product.create("어댑터테스트상품", new Money(15000), new Stock(50, 5), "음식");

        Product saved = adapter.save(product);
        Optional<Product> found = adapter.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("어댑터테스트상품");
    }

    @Test
    void findAll_전체_조회() {
        adapter.save(Product.create("상품1", new Money(1000), new Stock(10, 0), "A"));
        adapter.save(Product.create("상품2", new Money(2000), new Stock(20, 0), "B"));

        List<Product> products = adapter.findAll();

        assertThat(products.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void findByIdForUpdate_비관적_락_조회() {
        Product saved = adapter.save(Product.create("락테스트", new Money(5000), new Stock(30, 3), "C"));

        Optional<Product> found = adapter.findByIdForUpdate(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("락테스트");
    }

    @Test
    void Money_Stock_매핑_정합성() {
        Product product = Product.create("매핑테스트", new Money(12345), new Stock(67, 8), "D");

        Product saved = adapter.save(product);
        Product found = adapter.findById(saved.getId()).orElseThrow();

        assertThat(found.getPrice().amount()).isEqualTo(12345);
        assertThat(found.getStock().quantity()).isEqualTo(67);
        assertThat(found.getStock().threshold()).isEqualTo(8);
    }
}
