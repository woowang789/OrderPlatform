package com.orderplatform.gateway.config;

import com.orderplatform.gateway.config.jwt.JwtTokenValidator;
import com.orderplatform.gateway.config.security.InternalTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.path;

/**
 * API Gateway 라우팅 + 필터 설정
 * - 공개 경로: Internal Token만 주입 (JWT 검증 없음)
 * - 인증 경로: JWT 검증 + X-User-Id/X-User-Role 헤더 주입 + Internal Token 주입
 */
@Configuration
@RequiredArgsConstructor
public class GatewayRouteConfig {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final JwtTokenValidator jwtTokenValidator;
    private final InternalTokenGenerator internalTokenGenerator;

    // ── 공개 경로 (JWT 검증 없이 Internal Token만 주입) ──

    @Bean
    public RouterFunction<ServerResponse> publicMemberRoutes() {
        return route("public-member-signup")
                .POST("/api/members/signup", http())
                .filter(internalTokenOnlyFilter())
                .before(uri("http://localhost:8084"))
                .build()
            .and(
                route("public-member-login")
                .POST("/api/members/login", http())
                .filter(internalTokenOnlyFilter())
                .before(uri("http://localhost:8084"))
                .build()
            );
    }

    // ── 인증 필요 경로 (JWT 검증 + 헤더 주입) ──

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoutes() {
        return route("order-service")
                .route(path("/api/orders/**"), http())
                .filter(jwtAuthFilter())
                .before(uri("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoutes() {
        return route("payment-service")
                .route(path("/api/payments/**"), http())
                .filter(jwtAuthFilter())
                .before(uri("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> productServiceRoutes() {
        return route("product-service")
                .route(path("/api/products/**"), http())
                .filter(jwtAuthFilter())
                .before(uri("http://localhost:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> memberAuthRoutes() {
        return route("member-auth")
                .route(path("/api/members/**"), http())
                .filter(jwtAuthFilter())
                .before(uri("http://localhost:8084"))
                .build();
    }

    // ── 필터 정의 ──

    /**
     * JWT 검증 필터 — Authorization 헤더에서 Bearer 토큰 추출 → 검증 → 내부 헤더 주입
     */
    private HandlerFilterFunction<ServerResponse, ServerResponse> jwtAuthFilter() {
        return (request, next) -> {
            String token = extractBearerToken(request);
            if (token == null || !jwtTokenValidator.validateToken(token)) {
                return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
            }

            Long memberId = jwtTokenValidator.getMemberId(token);

            ServerRequest modified = ServerRequest.from(request)
                    .header(USER_ID_HEADER, memberId.toString())
                    .header(USER_ROLE_HEADER, "USER")
                    .header(INTERNAL_TOKEN_HEADER, internalTokenGenerator.generate())
                    .headers(headers -> headers.remove(AUTHORIZATION_HEADER))
                    .build();

            return next.handle(modified);
        };
    }

    /**
     * Internal Token만 주입하는 필터 — 공개 경로용 (JWT 검증 안 함)
     */
    private HandlerFilterFunction<ServerResponse, ServerResponse> internalTokenOnlyFilter() {
        return (request, next) -> {
            ServerRequest modified = ServerRequest.from(request)
                    .header(INTERNAL_TOKEN_HEADER, internalTokenGenerator.generate())
                    .build();

            return next.handle(modified);
        };
    }

    private String extractBearerToken(ServerRequest request) {
        String authorization = request.headers().firstHeader(AUTHORIZATION_HEADER);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
