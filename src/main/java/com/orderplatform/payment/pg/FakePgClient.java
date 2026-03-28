package com.orderplatform.payment.pg;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 가짜 PG 클라이언트 (Phase 1용)
 * 항상 결제 성공을 반환한다. Phase 2에서 Port/Adapter 패턴으로 전환 예정.
 */
@Component
public class FakePgClient {

    public PgPaymentResult processPayment(long amount, String method) {
        String pgTxnId = "PG-" + UUID.randomUUID();
        return new PgPaymentResult(pgTxnId, true, null);
    }
}
