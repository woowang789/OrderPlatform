package com.orderplatform.notification.adapter.out.notification;

import com.orderplatform.notification.application.port.out.NotificationSendPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 로그 기반 알림 발송 어댑터 (Phase 3).
 * 향후 이메일/SMS 어댑터로 교체 가능.
 */
@Slf4j
@Component
public class LogNotificationSendAdapter implements NotificationSendPort {

    @Override
    public void send(UUID memberId, String title, String message) {
        log.info("[알림] 수신자: {}, 제목: {}, 내용: {}", memberId, title, message);
    }
}
