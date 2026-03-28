package com.orderplatform.payment.controller;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.payment.dto.CreatePaymentRequest;
import com.orderplatform.payment.dto.PaymentResponse;
import com.orderplatform.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPayment(memberId, id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.cancelPayment(memberId, id));
    }
}
