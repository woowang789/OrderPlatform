package com.orderplatform.payment.adapter.in.web;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.payment.adapter.in.web.dto.CreatePaymentRequest;
import com.orderplatform.payment.adapter.in.web.dto.PaymentResponse;
import com.orderplatform.payment.application.port.in.*;
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

    private final RequestPaymentUseCase requestPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;
    private final GetPaymentUseCase getPaymentUseCase;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @CurrentMemberId Long memberId,
            @Valid @RequestBody CreatePaymentRequest request) {
        RequestPaymentCommand command = new RequestPaymentCommand(
                memberId, request.orderId(), request.method()
        );
        PaymentInfo info = requestPaymentUseCase.requestPayment(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(info));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPayment(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        PaymentInfo info = getPaymentUseCase.getPayment(memberId, id);
        return ResponseEntity.ok(PaymentResponse.from(info));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentResponse> cancelPayment(
            @CurrentMemberId Long memberId,
            @PathVariable UUID id) {
        CancelPaymentCommand command = new CancelPaymentCommand(memberId, id);
        PaymentInfo info = cancelPaymentUseCase.cancelPayment(command);
        return ResponseEntity.ok(PaymentResponse.from(info));
    }
}
