package com.orderplatform.payment.pg;

/**
 * PG사 결제 결과
 */
public record PgPaymentResult(
        String pgTxnId,
        boolean success,
        String failReason
) {
}
