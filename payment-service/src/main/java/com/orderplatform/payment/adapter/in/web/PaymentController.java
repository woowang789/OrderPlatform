package com.orderplatform.payment.adapter.in.web;

import com.orderplatform.common.annotation.CurrentMemberId;
import com.orderplatform.payment.adapter.in.web.dto.PaymentResponse;
import com.orderplatform.payment.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final GetPaymentUseCase getPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

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
