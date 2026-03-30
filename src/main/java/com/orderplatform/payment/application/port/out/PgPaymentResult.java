package com.orderplatform.payment.application.port.out;

/**
 * PG사 결제 결과
 */
public record PgPaymentResult(
        String pgTxnId,
        boolean success,
        String failReason
) {
}
