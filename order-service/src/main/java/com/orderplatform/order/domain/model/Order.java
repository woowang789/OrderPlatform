package com.orderplatform.order.domain.model;

import com.orderplatform.order.domain.exception.InvalidOrderStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 주문 Aggregate Root — 순수 Java 객체 (JPA/Spring 의존 없음)
 */
public class Order {

    private final UUID id;
    private final Long memberId;
    private OrderStatus status;
    private final long totalAmount;
    private final Long version;
    private final List<OrderLine> orderLines;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private Order(UUID id, Long memberId, OrderStatus status, long totalAmount,
                  Long version, List<OrderLine> orderLines,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.memberId = memberId;
        this.status = status;
        this.totalAmount = totalAmount;
        this.version = version;
        this.orderLines = orderLines;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 주문 생성 — CREATED 상태로 생성 후 즉시 PLACED로 전이
     * OrderLine 목록에서 totalAmount 자동 계산
     */
    public static Order create(Long memberId, List<OrderLine> orderLines) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다.");
        }

        long totalAmount = orderLines.stream()
                .mapToLong(line -> line.price() * line.quantity())
                .sum();

        if (totalAmount <= 0) {
            throw new IllegalArgumentException("주문 금액은 0보다 커야 합니다.");
        }

        Order order = new Order(null, memberId, OrderStatus.CREATED,
                totalAmount, null, List.copyOf(orderLines), null, null);
        order.place();
        return order;
    }

    /**
     * DB에서 복원 — 모든 필드를 포함한 완전한 상태 복원
     */
    public static Order reconstitute(UUID id, Long memberId, OrderStatus status,
                                     long totalAmount, Long version,
                                     List<OrderLine> orderLines,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Order(id, memberId, status, totalAmount, version,
                List.copyOf(orderLines), createdAt, updatedAt);
    }

    /**
     * 주문 확정 (CREATED → PLACED)
     */
    public void place() {
        if (this.status != OrderStatus.CREATED) {
            throw new InvalidOrderStatusException(this.status);
        }
        this.status = OrderStatus.PLACED;
    }

    /**
     * 결제 완료 처리 (PLACED → PAID)
     */
    public void markPaid() {
        if (this.status != OrderStatus.PLACED) {
            throw new InvalidOrderStatusException(this.status);
        }
        this.status = OrderStatus.PAID;
    }

    /**
     * 주문 확정 — 재고 차감 완료 (PAID → CONFIRMED)
     */
    public void markConfirmed() {
        if (this.status != OrderStatus.PAID) {
            throw new InvalidOrderStatusException(this.status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    /**
     * 주문 취소 (PLACED 또는 PAID 또는 CONFIRMED → CANCELLED)
     */
    public void cancel() {
        if (this.status != OrderStatus.PLACED && this.status != OrderStatus.PAID && this.status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStatusException(this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public UUID getId() {
        return id;
    }

    public Long getMemberId() {
        return memberId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public Long getVersion() {
        return version;
    }

    public List<OrderLine> getOrderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
