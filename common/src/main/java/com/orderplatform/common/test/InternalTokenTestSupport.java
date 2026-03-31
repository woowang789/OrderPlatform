package com.orderplatform.common.test;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 통합 테스트에서 Internal Token 헤더를 주입하는 유틸리티.
 * application-test.yml의 internal-token.secret과 동일한 키를 사용한다.
 */
public final class InternalTokenTestSupport {

    private static final String TEST_SECRET = "b3JkZXJwbGF0Zm9ybS1pbnRlcm5hbC10b2tlbi1zZWNyZXQta2V5LWZvci1obWFjLXNoYTI1Ng==";
    private static final String ALGORITHM = "HmacSHA256";

    private InternalTokenTestSupport() {
    }

    /**
     * MockMvc 요청에 유효한 X-Internal-Token 헤더를 주입하는 RequestPostProcessor
     */
    public static RequestPostProcessor internalToken() {
        return request -> {
            request.addHeader("X-Internal-Token", generateToken());
            return request;
        };
    }

    /**
     * MockMvc 요청에 X-Internal-Token + X-User-Id 헤더를 함께 주입
     */
    public static RequestPostProcessor internalTokenWithUser(Long memberId) {
        return request -> {
            request.addHeader("X-Internal-Token", generateToken());
            request.addHeader("X-User-Id", memberId.toString());
            return request;
        };
    }

    private static String generateToken() {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String signature = sign(timestamp);
        return Base64.getEncoder().encodeToString(
                (timestamp + "." + signature).getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String sign(String data) {
        try {
            byte[] secretKeyBytes = Base64.getDecoder().decode(TEST_SECRET);
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKeyBytes, ALGORITHM));
            byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("테스트 Internal Token 생성 실패", e);
        }
    }
}
