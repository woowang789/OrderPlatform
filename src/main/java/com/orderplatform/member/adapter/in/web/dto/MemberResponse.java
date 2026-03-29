package com.orderplatform.member.adapter.in.web.dto;

import com.orderplatform.member.application.port.in.MemberInfo;

import java.time.LocalDateTime;

public record MemberResponse(
        Long id,
        String email,
        String name,
        LocalDateTime createdAt
) {
    public static MemberResponse from(MemberInfo info) {
        return new MemberResponse(
                info.id(),
                info.email(),
                info.name(),
                info.createdAt()
        );
    }
}
