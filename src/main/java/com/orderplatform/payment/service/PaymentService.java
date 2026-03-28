package com.orderplatform.payment.service;

import com.orderplatform.common.exception.*;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderStatus;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.order.service.OrderService;
import com.orderplatform.payment.dto.CreatePaymentRequest;
import com.orderplatform.payment.dto.PaymentResponse;
import com.orderplatform.payment.entity.Payment;
import com.orderplatform.payment.entity.PaymentMethod;
import com.orderplatform.payment.entity.PaymentStatus;
import com.orderplatform.payment.pg.FakePgClient;
import com.orderplatform.payment.pg.PgPaymentResult;
import com.orderplatform.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 서비스 (Phase 1: 동기 호출 + 강결합)
 * OrderRepository, OrderService를 직접 참조 — Phase 2에서 이벤트 기반으로 디커플링 예정
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final FakePgClient fakePgClient;

    /**
     * 결제 요청: 주문 검증 → 중복 확인 → PG 호출 → 상태 전이
     */
    @Transactional
    public PaymentResponse createPayment(Long memberId, CreatePaymentRequest request) {
        // 주문 조회 및 소유자 검증
        Order order = orderRepository.findByIdWithOrderLines(request.orderId())
                .orElseThrow(() -> new OrderNotFoundException(request.orderId()));

        if (!order.getMemberId().equals(memberId)) {
            throw new OrderNotFoundException(request.orderId());
        }

        // 중복 결제 방지 (상태 검증보다 먼저 — "이미 결제됨"이 더 명확한 피드백)
        paymentRepository.findByOrderIdAndStatusNot(request.orderId(), PaymentStatus.CANCELLED)
                .ifPresent(p -> {
                    throw new DuplicatePaymentException(request.orderId());
                });

        // 주문 상태 확인 (PLACED만 결제 가능)
        if (order.getStatus() != OrderStatus.PLACED) {
            throw new InvalidOrderStatusException(order.getStatus());
        }

        // 결제 생성
        PaymentMethod method = PaymentMethod.valueOf(request.method());
        Payment payment = Payment.create(
                order.getId(), memberId, order.getTotalAmount(), method
        );

        // PG 결제 처리
        PgPaymentResult pgResult = fakePgClient.processPayment(
                order.getTotalAmount(), request.method()
        );

        if (pgResult.success()) {
            payment.complete(pgResult.pgTxnId());
            order.markPaid();
        } else {
            payment.fail(pgResult.failReason());
        }

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    /**
     * 결제 조회 (본인 결제만)
     */
    public PaymentResponse getPayment(Long memberId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소: 결제 취소 → 주문 취소 + 재고 복원
     */
    @Transactional
    public PaymentResponse cancelPayment(Long memberId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndMemberId(paymentId, memberId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        payment.cancel();
        orderService.cancelOrder(memberId, payment.getOrderId());

        return PaymentResponse.from(payment);
    }
}
