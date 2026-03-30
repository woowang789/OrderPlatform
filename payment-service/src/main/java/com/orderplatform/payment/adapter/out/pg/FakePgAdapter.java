package com.orderplatform.payment.adapter.out.pg;

import com.orderplatform.payment.application.port.out.PaymentGatewayPort;
import com.orderplatform.payment.application.port.out.PgPaymentResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 가짜 PG Adapter (Phase 1~2용)
 * 항상 결제 성공을 반환한다. 실제 PG 연동 시 이 Adapter만 교체하면 됨.
 */
@Component
public class FakePgAdapter implements PaymentGatewayPort {

    @Override
    public PgPaymentResult processPayment(long amount, String method) {
        String pgTxnId = "PG-" + UUID.randomUUID();
        return new PgPaymentResult(pgTxnId, true, null);
    }
}
