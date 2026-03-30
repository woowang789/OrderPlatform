package com.orderplatform.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderplatform.order.common.AbstractIntegrationTest;
import com.orderplatform.order.adapter.in.web.dto.CreateOrderRequest;
import com.orderplatform.order.adapter.in.web.dto.OrderItemRequest;
import com.orderplatform.order.adapter.out.persistence.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Collections;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
    private OrderJpaRepository orderRepository;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    /**
     * MockMvc 요청에 memberId를 인증 정보로 주입
     */
    private static RequestPostProcessor memberAuth(Long memberId) {
        return authentication(
                new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList())
        );
    }

    @Test
    @DisplayName("주문 생성 성공 — Stub 어댑터 사용")
    void createOrder_success() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(1L, 3))
        );

        mockMvc.perform(post("/api/orders")
                        .with(memberAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.orderLines.length()").value(1));
    }

    @Test
    @DisplayName("주문 취소 성공")
    void cancelOrder_success() throws Exception {
        String orderId = createOrderViaApi(1L, 5);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("주문 상세 조회 성공")
    void getOrder_success() throws Exception {
        String orderId = createOrderViaApi(1L, 2);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.status").value("PLACED"));
    }

    @Test
    @DisplayName("내 주문 목록 조회")
    void getMyOrders_success() throws Exception {
        createOrderViaApi(1L, 1);
        createOrderViaApi(1L, 2);

        mockMvc.perform(get("/api/orders")
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("타인 주문 조회 시 404")
    void getOrder_accessDenied() throws Exception {
        String orderId = createOrderViaApi(1L, 1);

        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .with(memberAuth(2L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("타인 주문 취소 시 404")
    void cancelOrder_accessDenied() throws Exception {
        String orderId = createOrderViaApi(1L, 1);

        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .with(memberAuth(2L)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("전체 플로우: 주문 생성 → 조회 → 목록 → 취소")
    void fullFlow() throws Exception {
        // 1. 주문 생성
        String orderId = createOrderViaApi(1L, 3);

        // 2. 주문 상세 조회
        mockMvc.perform(get("/api/orders/{id}", orderId)
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"));

        // 3. 내 주문 목록
        mockMvc.perform(get("/api/orders")
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // 4. 주문 취소
        mockMvc.perform(post("/api/orders/{id}/cancel", orderId)
                        .with(memberAuth(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    // === 헬퍼 메서드 ===

    private String createOrderViaApi(Long productId, int quantity) throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new OrderItemRequest(productId, quantity))
        );

        String response = mockMvc.perform(post("/api/orders")
                        .with(memberAuth(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("id").asText();
    }
}
