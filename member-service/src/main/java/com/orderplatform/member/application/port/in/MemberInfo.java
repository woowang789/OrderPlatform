package com.orderplatform.member.application.port.in;

import java.time.LocalDateTime;

public record MemberInfo(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt
) {
}
