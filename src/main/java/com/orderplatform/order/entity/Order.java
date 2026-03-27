package com.orderplatform.order.entity;

import com.orderplatform.common.entity.BaseEntity;
import com.orderplatform.common.exception.InvalidOrderStatusException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문 Aggregate Root 엔티티
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false)
    private long totalAmount;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    private Order(Long memberId, List<OrderLine> orderLines, long totalAmount) {
        this.memberId = memberId;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.CREATED;
        orderLines.forEach(this::addOrderLine);
    }

    /**
     * 주문 생성 정적 팩토리
     */
    public static Order create(Long memberId, List<OrderLine> orderLines, long totalAmount) {
        if (orderLines == null || orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목은 최소 1개 이상이어야 합니다.");
        }
        if (totalAmount <= 0) {
            throw new IllegalArgumentException("주문 금액은 0보다 커야 합니다.");
        }
        return new Order(memberId, orderLines, totalAmount);
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
     * 주문 취소 (PLACED 또는 PAID → CANCELLED)
     */
    public void cancel() {
        if (this.status != OrderStatus.PLACED && this.status != OrderStatus.PAID) {
            throw new InvalidOrderStatusException(this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }

    private void addOrderLine(OrderLine orderLine) {
        this.orderLines.add(orderLine);
        orderLine.setOrder(this);
    }
}
