package com.orderplatform.member.application.port.out;

public interface TokenGeneratorPort {

    String generateToken(Long memberId);
}
