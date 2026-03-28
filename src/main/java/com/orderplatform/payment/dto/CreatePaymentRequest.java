package com.orderplatform.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull(message = "주문 ID는 필수입니다.")
        UUID orderId,

        @NotBlank(message = "결제 수단은 필수입니다.")
        String method
) {
}
