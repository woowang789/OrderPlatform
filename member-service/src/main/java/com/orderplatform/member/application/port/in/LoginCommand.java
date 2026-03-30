package com.orderplatform.member.application.port.in;

public record LoginCommand(
        String email,
        String password
) {
}
