package com.orderplatform.gateway.config.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Internal Token 생성기 — Gateway가 하위 서비스 요청 시 주입하는 단기 토큰
 * 형식: Base64(timestamp.signature) — HMAC-SHA256 서명, TTL 30초
 */
@Component
public class InternalTokenGenerator {

    private static final String ALGORITHM = "HmacSHA256";

    @Value("${internal-token.secret}")
    private String secret;

    private byte[] secretKeyBytes;

    @PostConstruct
    public void init() {
        this.secretKeyBytes = Base64.getDecoder().decode(secret);
    }

    public String generate() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = sign(timestamp);
        return Base64.getEncoder().encodeToString(
                (timestamp + "." + signature).getBytes(StandardCharsets.UTF_8)
        );
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Internal Token 서명 생성 실패", e);
        }
    }
}
