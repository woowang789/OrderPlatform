package com.orderplatform.common.domain.event;

import java.time.LocalDateTime;

/**
 * 모든 도메인 이벤트의 기반 인터페이스.
 * 순수 Java — 프레임워크 의존 없음.
 */
public interface DomainEvent {

    LocalDateTime occurredAt();
}
