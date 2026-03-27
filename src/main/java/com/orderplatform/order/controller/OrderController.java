package com.orderplatform.order.controller;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.order.dto.CreateOrderRequest;
import com.orderplatform.order.dto.OrderResponse;
import com.orderplatform.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.getOrder(memberId, id));
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @CurrentMemberId Long memberId) {
        return ResponseEntity.ok(orderService.getMyOrders(memberId));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(orderService.cancelOrder(memberId, id));
    }
}
