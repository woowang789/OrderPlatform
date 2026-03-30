package com.orderplatform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.member.adapter.out.persistence.MemberJpaRepository;
import com.orderplatform.order.adapter.out.persistence.OrderJpaRepository;
import com.orderplatform.payment.adapter.out.persistence.PaymentJpaRepository;
import com.orderplatform.product.adapter.out.persistence.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E 전체 플로우 통합 테스트
 * 회원가입 → 로그인 → 상품 등록 → 주문 생성 → 결제 → 상태 확인
 * Phase 1과 동일한 기능이 헥사고날 구조에서 100% 동작함을 증명한다.
 */
class FullFlowE2ETest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberJpaRepository memberRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private OrderJpaRepository orderRepository;

    @Autowired
    private PaymentJpaRepository paymentRepository;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("전체 플로우: 회원가입 → 로그인 → 상품 등록 → 주문 생성 → 결제 → 주문 상태 PAID 확인")
    void fullBusinessFlow() throws Exception {
        // 1. 회원가입
        String signupBody = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "name": "테스트유저"
                }
                """;

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트유저"));

        // 2. 로그인 → JWT 토큰 획득
        String loginBody = """
                {
                    "email": "test@example.com",
                    "password": "password123"
                }
                """;

        String loginResponse = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        // 3. 내 정보 조회
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"));

        // 4. 상품 등록
        String productBody = """
                {
                    "name": "맥북 프로 16인치",
                    "price": 3500000,
                    "stock": 50,
                    "category": "전자제품"
                }
                """;

        String productResponse = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("맥북 프로 16인치"))
                .andExpect(jsonPath("$.price").value(3500000))
                .andExpect(jsonPath("$.stock").value(50))
                .andReturn().getResponse().getContentAsString();

        long productId = objectMapper.readTree(productResponse).get("id").asLong();

        // 5. 상품 조회 (비인증)
        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("맥북 프로 16인치"));

        // 6. 주문 생성
        String orderBody = String.format("""
                {
                    "items": [
                        {"productId": %d, "quantity": 2}
                    ]
                }
                """, productId);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(7000000))
                .andExpect(jsonPath("$.orderLines.length()").value(1))
                .andExpect(jsonPath("$.orderLines[0].productName").value("맥북 프로 16인치"))
                .andExpect(jsonPath("$.orderLines[0].quantity").value(2))
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(orderResponse).get("id").asText();

        // 재고 차감 확인: 50 - 2 = 48
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(48);

        // 7. 결제 요청
        String paymentBody = String.format("""
                {
                    "orderId": "%s",
                    "method": "CARD"
                }
                """, orderId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(7000000))
                .andExpect(jsonPath("$.method").value("CARD"));

        // 8. 주문 상태 확인 → PAID
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(7000000));

        // 9. 내 주문 목록 조회
        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("전체 플로우: 주문 생성 → 결제 전 취소 → 재고 복원")
    void fullFlow_cancelBeforePayment() throws Exception {
        // 회원가입 + 로그인
        String token = signupAndLogin("cancel@example.com", "password123", "취소테스트");

        // 상품 등록
        long productId = createProduct(token, "취소 테스트 상품", 20000, 30, "테스트");

        // 주문 생성
        String orderId = createOrder(token, productId, 5);
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(25);

        // 주문 취소
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // 재고 복원 확인: 30 - 5 + 5 = 30
        assertThat(productRepository.findById(productId).orElseThrow().getStock()).isEqualTo(30);
    }

    @Test
    @DisplayName("다중 상품 주문 → 결제 → 각 상품 재고 차감 확인")
    void fullFlow_multipleProducts() throws Exception {
        String token = signupAndLogin("multi@example.com", "password123", "다중상품");

        long productA = createProduct(token, "상품A", 10000, 100, "음식");
        long productB = createProduct(token, "상품B", 25000, 50, "의류");

        // 다중 상품 주문
        String orderBody = String.format("""
                {
                    "items": [
                        {"productId": %d, "quantity": 3},
                        {"productId": %d, "quantity": 2}
                    ]
                }
                """, productA, productB);

        String orderResponse = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.totalAmount").value(80000))
                .andExpect(jsonPath("$.orderLines.length()").value(2))
                .andReturn().getResponse().getContentAsString();

        String orderId = objectMapper.readTree(orderResponse).get("id").asText();

        // 재고 차감 확인
        assertThat(productRepository.findById(productA).orElseThrow().getStock()).isEqualTo(97);
        assertThat(productRepository.findById(productB).orElseThrow().getStock()).isEqualTo(48);

        // 결제
        String paymentBody = String.format("""
                {"orderId": "%s", "method": "BANK_TRANSFER"}
                """, orderId);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(paymentBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.amount").value(80000));

        // 주문 상태 PAID 확인
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    // === 헬퍼 메서드 ===

    private String signupAndLogin(String email, String password, String name) throws Exception {
        String signupBody = String.format("""
                {"email": "%s", "password": "%s", "name": "%s"}
                """, email, password, name);

        mockMvc.perform(post("/api/members/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody))
                .andExpect(status().isCreated());

        String loginBody = String.format("""
                {"email": "%s", "password": "%s"}
                """, email, password);

        String response = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private long createProduct(String token, String name, long price, int stock, String category) throws Exception {
        String body = String.format("""
                {"name": "%s", "price": %d, "stock": %d, "category": "%s"}
                """, name, price, stock, category);

        String response = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private String createOrder(String token, long productId, int quantity) throws Exception {
        String body = String.format("""
                {"items": [{"productId": %d, "quantity": %d}]}
                """, productId, quantity);

        String response = mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asText();
    }
}
