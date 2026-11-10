package com.haui.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * AuthenticationFilter cho API Gateway
 * Kiểm tra tính hợp lệ của JSON Web Token (JWT) theo mô hình OHS (Open Host Service) phi tập trung.
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final SecretKey key;

    public AuthenticationFilter(@Value("${jwt.secret}") String secret) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Request tới endpoint bảo vệ [{}] thiếu Authorization Header", request.getURI().getPath());
                return onError(exchange, "Thiếu Authorization header trong request", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Authorization header không đúng định dạng Bearer", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String username = claims.getSubject();
                Object userId = claims.get("userId");
                Object role = claims.get("role");

                log.info("JWT xác thực thành công cho người dùng: {} (Role: {}) tại Gateway", username, role);

                // Chuyển tiếp định danh người dùng xuống downstream microservices
                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Name", username)
                        .header("X-User-Id", userId != null ? userId.toString() : "")
                        .header("X-User-Role", role != null ? role.toString() : "")
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (Exception ex) {
                log.warn("Xác thực JWT thất bại: {}", ex.getMessage());
                return onError(exchange, "JWT Token không hợp lệ hoặc đã hết hạn!", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format("{\"timestamp\": \"%s\", \"status\": %d, \"error\": \"%s\", \"message\": \"%s\"}",
                java.time.Instant.now(), httpStatus.value(), httpStatus.getReasonPhrase(), errMessage);

        DataBuffer buffer = response.bufferFactory().wrap(jsonError.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Cấu hình mở rộng nếu cần
    }
}
