package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 모든 도메인 이벤트의 기반 인터페이스.
 * 순수 Java — 프레임워크 의존 없음.
 *
 * Kafka 전송에 필요한 메타데이터를 default 메서드로 제공하여
 * 기존 이벤트 구현체와의 하위 호환성을 유지한다.
 */
public interface DomainEvent {

    /** 이벤트 발생 시각 */
    LocalDateTime occurredAt();

    /** 이벤트 고유 식별자 */
    default UUID eventId() { return null; }

    /** 이벤트 타입 (클래스 이름) */
    default String eventType() { return getClass().getSimpleName(); }

    /** 이벤트 스키마 버전 */
    default int version() { return 1; }

    /** Aggregate Root 식별자 */
    default UUID aggregateId() { return null; }

    /** Kafka 파티션 키 — 모든 토픽에서 orderId 기준으로 순서 보장 */
    default UUID orderId() { return null; }
}
