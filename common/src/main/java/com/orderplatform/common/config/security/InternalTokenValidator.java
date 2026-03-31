package com.orderplatform.common.config.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Internal Token 검증기 — HMAC-SHA256 서명 검증 + TTL 30초
 * Gateway가 생성한 토큰을 각 하위 서비스에서 검증
 */
@Component
@ConditionalOnProperty("internal-token.secret")
public class InternalTokenValidator {

    private static final String ALGORITHM = "HmacSHA256";
    private static final long TTL_MILLIS = 30_000;

    @Value("${internal-token.secret}")
    private String secret;

    private byte[] secretKeyBytes;

    @PostConstruct
    public void init() {
        this.secretKeyBytes = Base64.getDecoder().decode(secret);
    }

    public boolean validate(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            int dotIndex = decoded.indexOf('.');
            if (dotIndex < 0) {
                return false;
            }

            String timestamp = decoded.substring(0, dotIndex);
            String signature = decoded.substring(dotIndex + 1);

            // 서명 검증 (constant-time comparison)
            String expectedSignature = sign(timestamp);
            if (!MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // TTL 검증 (30초)
            long tokenTime = Long.parseLong(timestamp);
            return System.currentTimeMillis() - tokenTime <= TTL_MILLIS;
        } catch (Exception e) {
            return false;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Internal Token 서명 검증 실패", e);
        }
    }
}
