package com.orderplatform.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.config.jwt.JwtTokenProvider;
import com.orderplatform.order.dto.CreateOrderRequest;
import com.orderplatform.order.dto.OrderItemRequest;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.payment.dto.CreatePaymentRequest;
import com.orderplatform.payment.repository.PaymentRepository;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

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
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        tokenA = jwtTokenProvider.generateToken(1L);
        tokenB = jwtTokenProvider.generateToken(2L);
    }

    @Test
    @DisplayName("결제 성공 — 결제 COMPLETED + 주문 PAID")
    void createPayment_success() throws Exception {
        Product product = createTestProduct("결제 상품", 10000, 100);
        String orderId = createOrderViaApi(tokenA, product.getId(), 3);

        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                UUID.fromString(orderId), "CARD"
        );

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.amount").value(30000))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.method").value("CARD"))
                .andExpect(jsonPath("$.pgTxnId").isNotEmpty());

        // 주문 상태 PAID 확인
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("중복 결제 시 409 CONFLICT")
    void createPayment_duplicate() throws Exception {
        Product product = createTestProduct("중복 상품", 5000, 50);
        String orderId = createOrderViaApi(tokenA, product.getId(), 2);

        // 첫 번째 결제 성공
        createPaymentViaApi(tokenA, orderId, "CARD");

        // 두 번째 결제 시도 → CONFLICT
        CreatePaymentRequest duplicateRequest = new CreatePaymentRequest(
                UUID.fromString(orderId), "BANK_TRANSFER"
        );

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PLACED가 아닌 주문에 결제 시 400")
    void createPayment_invalidOrderStatus() throws Exception {
        Product product = createTestProduct("상태 상품", 10000, 100);
        String orderId = createOrderViaApi(tokenA, product.getId(), 1);

        // 주문 취소
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        // 취소된 주문에 결제 시도
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                UUID.fromString(orderId), "CARD"
        );

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(paymentRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("결제 조회 성공")
    void getPayment_success() throws Exception {
        Product product = createTestProduct("조회 상품", 8000, 30);
        String orderId = createOrderViaApi(tokenA, product.getId(), 1);
        String paymentId = createPaymentViaApi(tokenA, orderId, "CARD");

        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentId))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("타인 결제 조회 시 404")
    void getPayment_accessDenied() throws Exception {
        Product product = createTestProduct("타인 상품", 10000, 50);
        String orderId = createOrderViaApi(tokenA, product.getId(), 1);
        String paymentId = createPaymentViaApi(tokenA, orderId, "CARD");

        // 회원B가 회원A의 결제 조회
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("결제 취소 — 결제 CANCELLED + 주문 CANCELLED + 재고 복원")
    void cancelPayment_success() throws Exception {
        Product product = createTestProduct("취소 상품", 5000, 50);
        String orderId = createOrderViaApi(tokenA, product.getId(), 5);
        String paymentId = createPaymentViaApi(tokenA, orderId, "CARD");

        // 재고 확인: 50 - 5 = 45
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(45);

        // 결제 취소
        mockMvc.perform(post("/api/payments/{id}/cancel", paymentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 주문 상태 CANCELLED 확인
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 재고 복원 확인: 45 + 5 = 50
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
    }

    @Test
    @DisplayName("존재하지 않는 결제 조회 시 404")
    void getPayment_notFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(get("/api/payments/{id}", randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("존재하지 않는 결제 취소 시 404")
    void cancelPayment_notFound() throws Exception {
        UUID randomId = UUID.randomUUID();

        mockMvc.perform(post("/api/payments/{id}/cancel", randomId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("미인증 결제 요청 시 401")
    void createPayment_unauthorized() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.randomUUID(), "CARD");

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("전체 플로우: 주문 → 결제 → 조회 → 취소 → 재고 복원")
    void fullFlow() throws Exception {
        // 1. 상품 등록
        Product product = createTestProduct("플로우 상품", 20000, 50);

        // 2. 주문 생성
        String orderId = createOrderViaApi(tokenA, product.getId(), 3);
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(47);

        // 3. 결제 요청
        String paymentId = createPaymentViaApi(tokenA, orderId, "CARD");

        // 4. 주문 상태 PAID 확인
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.status").value("PAID"));

        // 5. 결제 조회
        mockMvc.perform(get("/api/payments/{id}", paymentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(60000));

        // 6. 결제 취소
        mockMvc.perform(post("/api/payments/{id}/cancel", paymentId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 7. 주문 상태 CANCELLED 확인
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 8. 재고 복원 확인
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
    }

    // === 헬퍼 메서드 ===

    private Product createTestProduct(String name, int price, int stock) {
        return productRepository.save(new Product(name, price, stock, "전자제품"));
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

        return objectMapper.readTree(response).get("id").asText();
    }

    private String createPaymentViaApi(String token, String orderId, String method) throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(
                UUID.fromString(orderId), method
        );

        String response = mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
