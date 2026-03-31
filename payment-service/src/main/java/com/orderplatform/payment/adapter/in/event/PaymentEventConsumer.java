package com.orderplatform.payment.adapter.in.event;

import com.orderplatform.common.domain.event.OrderPlacedEvent;
import com.orderplatform.common.domain.event.PaymentCancelRequestedEvent;
import com.orderplatform.common.domain.event.Topics;
import com.orderplatform.common.domain.event.payload.StockItemPayload;
import com.orderplatform.payment.application.port.in.CancelPaymentUseCase;
import com.orderplatform.payment.application.port.in.RequestPaymentCommand;
import com.orderplatform.payment.application.port.in.RequestPaymentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka 이벤트 소비 어댑터 — 결제 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final RequestPaymentUseCase requestPaymentUseCase;
    private final CancelPaymentUseCase cancelPaymentUseCase;

    /**
     * 주문 생성 이벤트 소비 → 결제 처리
     */
    @KafkaListener(topics = Topics.ORDER_PLACED, groupId = "payment-group")
    public void handleOrderPlaced(OrderPlacedEvent event) {
        log.info("주문 생성 이벤트 수신 — orderId: {}", event.orderId());

        List<StockItemPayload> stockItems = event.items().stream()
                .map(item -> new StockItemPayload(item.productId(), item.quantity()))
                .toList();

        RequestPaymentCommand command = new RequestPaymentCommand(
                event.memberId(), event.orderId(), event.totalAmount(),
                event.paymentMethod(), stockItems);

        requestPaymentUseCase.requestPayment(command);
    }

    /**
     * 결제 취소 요청 이벤트 소비 → 결제 취소 (Saga 보상)
     */
    @KafkaListener(topics = Topics.PAYMENT_CANCEL_REQUESTED, groupId = "payment-group")
    public void handlePaymentCancelRequested(PaymentCancelRequestedEvent event) {
        log.info("결제 취소 요청 이벤트 수신 — orderId: {}", event.orderId());
        cancelPaymentUseCase.cancelPaymentByOrderId(event.orderId());
    }
}
