package com.orderplatform.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.config.jwt.JwtTokenProvider;
import com.orderplatform.order.dto.CreateOrderRequest;
import com.orderplatform.order.dto.OrderItemRequest;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        tokenA = jwtTokenProvider.generateToken(1L);
        tokenB = jwtTokenProvider.generateToken(2L);
    }

    @Test
    @DisplayName("주문 생성 성공 — 재고 차감 확인")
    void createOrder_success() throws Exception {
        Product product = createTestProduct("테스트 상품", 10000, 100, "전자제품");

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(product.getId(), 3))
        );

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(30000))
                .andExpect(jsonPath("$.orderLines.length()").value(1))
                .andExpect(jsonPath("$.orderLines[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.orderLines[0].productName").value("테스트 상품"))
                .andExpect(jsonPath("$.orderLines[0].price").value(10000))
                .andExpect(jsonPath("$.orderLines[0].quantity").value(3));

        // 재고 차감 확인
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(97);
    }

    @Test
    @DisplayName("주문 취소 성공 — 재고 복원 확인")
    void cancelOrder_success() throws Exception {
        Product product = createTestProduct("취소 상품", 5000, 50, "음식");
        String orderId = createOrderViaApi(tokenA, product.getId(), 5);

        // 취소
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 재고 복원 확인: 50 - 5 + 5 = 50
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("재고 부족 시 주문 실패")
    void createOrder_insufficientStock() throws Exception {
        Product product = createTestProduct("부족 상품", 8000, 2, "음료");

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(product.getId(), 5))
        );

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // 재고 변동 없음 확인
        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 상품으로 주문 실패")
    void createOrder_productNotFound() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(999L, 1))
        );

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrder_success() throws Exception {
        Product product = createTestProduct("조회 상품", 15000, 30, "의류");
        String orderId = createOrderViaApi(tokenA, product.getId(), 2);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(30000));
    }

    @Test
    @DisplayName("내 주문 목록 조회")
    void getMyOrders_success() throws Exception {
        Product product = createTestProduct("목록 상품", 5000, 100, "음식");
        createOrderViaApi(tokenA, product.getId(), 1);
        createOrderViaApi(tokenA, product.getId(), 2);

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("타인 주문 조회 시 404")
    void getOrder_accessDenied() throws Exception {
        Product product = createTestProduct("타인 상품", 10000, 50, "전자제품");
        String orderId = createOrderViaApi(tokenA, product.getId(), 1);

        // 회원B가 회원A의 주문 조회 시도
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("타인 주문 취소 시 404")
    void cancelOrder_accessDenied() throws Exception {
        Product product = createTestProduct("타인 취소 상품", 10000, 50, "전자제품");
        String orderId = createOrderViaApi(tokenA, product.getId(), 1);

        // 회원B가 회원A의 주문 취소 시도
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("미인증 주문 생성 시 401")
    void createOrder_unauthorized() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 플로우: 주문 생성 → 조회 → 목록 → 취소 → 재고 복원")
    void fullFlow() throws Exception {
        // 1. 상품 등록
        Product product = createTestProduct("플로우 상품", 20000, 50, "전자제품");

        // 2. 주문 생성
        String orderId = createOrderViaApi(tokenA, product.getId(), 3);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(47);

        // 3. 주문 상세 조회
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(60000));

        // 4. 내 주문 목록
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 5. 주문 취소
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 6. 재고 복원 확인
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
    }

    // === 헬퍼 메서드 ===

    private Product createTestProduct(String name, int price, int stock, String category) {
        return productRepository.save(new Product(name, price, stock, category));
    }

    private String createOrderViaApi(String token, Long productId, int quantity) throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(productId, quantity))
        );

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asText();
    }
}
