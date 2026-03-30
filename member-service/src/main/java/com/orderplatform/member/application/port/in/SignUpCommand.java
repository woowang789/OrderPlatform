package com.orderplatform.member.application.port.in;

public record SignUpCommand(
        String email,
        String password,
        String name
) {
}
