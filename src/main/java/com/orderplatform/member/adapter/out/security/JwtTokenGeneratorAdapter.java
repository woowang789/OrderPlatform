package com.orderplatform.member.adapter.out.security;

import com.orderplatform.config.jwt.JwtTokenProvider;
import com.orderplatform.member.application.port.out.TokenGeneratorPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenGeneratorAdapter implements TokenGeneratorPort {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public String generateToken(Long memberId) {
        return jwtTokenProvider.generateToken(memberId);
    }
}
