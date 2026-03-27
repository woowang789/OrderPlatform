package com.orderplatform.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.common.exception.InsufficientStockException;
import com.orderplatform.config.jwt.JwtTokenProvider;
import com.orderplatform.product.dto.CreateProductRequest;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import com.orderplatform.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
        token = jwtTokenProvider.generateToken(1L);
    }

    @Test
    @DisplayName("상품 등록 성공")
    void createProduct_success() throws Exception {
        CreateProductRequest request = new CreateProductRequest("테스트 상품", 10000, 100, "전자제품");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("테스트 상품"))
                .andExpect(jsonPath("$.price").value(10000))
                .andExpect(jsonPath("$.stock").value(100))
                .andExpect(jsonPath("$.category").value("전자제품"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("미인증 상품 등록 실패")
    void createProduct_unauthorized() throws Exception {
        CreateProductRequest request = new CreateProductRequest("테스트 상품", 10000, 100, "전자제품");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("상품 등록 유효성 검증 실패")
    void createProduct_invalidInput() throws Exception {
        CreateProductRequest request = new CreateProductRequest("", -1000, -5, null);

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 목록 조회 성공 (비인증)")
    void getProducts_success() throws Exception {
        // 상품 2개 등록
        createTestProduct("상품A", 5000, 10, "음식");
        createTestProduct("상품B", 8000, 20, "음료");

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("상품A"))
                .andExpect(jsonPath("$[1].name").value("상품B"));
    }

    @Test
    @DisplayName("상품 상세 조회 성공")
    void getProduct_success() throws Exception {
        Product product = createTestProduct("조회 상품", 15000, 50, "의류");

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId()))
                .andExpect(jsonPath("$.name").value("조회 상품"))
                .andExpect(jsonPath("$.price").value(15000))
                .andExpect(jsonPath("$.stock").value(50))
                .andExpect(jsonPath("$.category").value("의류"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 404")
    void getProduct_notFound() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("재고 차감 성공")
    void decreaseStock_success() {
        Product product = createTestProduct("재고 상품", 10000, 100, "전자제품");

        productService.decreaseStock(product.getId(), 30);

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(70);
    }

    @Test
    @DisplayName("재고 부족 시 예외 발생")
    void decreaseStock_insufficientStock() {
        Product product = createTestProduct("재고 부족 상품", 5000, 10, "음식");

        assertThatThrownBy(() -> productService.decreaseStock(product.getId(), 20))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    @DisplayName("전체 플로우: 등록 → 목록 → 상세 → 재고 차감 → 재조회")
    void fullFlow() throws Exception {
        // 1. 상품 등록
        CreateProductRequest request = new CreateProductRequest("플로우 상품", 20000, 50, "전자제품");

        String createResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(createResponse).get("id").asLong();

        // 2. 목록 조회
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. 상세 조회
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(50));

        // 4. 재고 차감
        productService.decreaseStock(productId, 15);

        // 5. 재조회 — 재고 감소 확인
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(35));
    }

    // 헬퍼 메서드
    private Product createTestProduct(String name, int price, int stock, String category) {
        Product product = new Product(name, price, stock, category);
        return productRepository.save(product);
    }
}
