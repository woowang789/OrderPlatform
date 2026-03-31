package com.orderplatform.notification.application.port.out;

import java.util.UUID;

/**
 * 알림 발송 Output Port.
 * 구현체를 교체하여 로그/이메일/SMS 등 다양한 채널로 알림을 발송할 수 있다.
 */
public interface NotificationSendPort {

    void send(UUID memberId, String title, String message);
}
