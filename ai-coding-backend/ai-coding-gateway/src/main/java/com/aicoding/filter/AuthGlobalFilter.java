package com.aicoding.filter;

import com.aicoding.common.ErrorCode;
import com.aicoding.core.auth.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 网关全局鉴权过滤器：JWT 校验通过后向下游服务透传用户身份头
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> WHITE_LIST = List.of(
            "/user/register", "/user/login", "/preview/", "/s/", "/cover/", "/template/", "/actuator", "/error");

    private final JwtUtils jwtUtils;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        // 统一剥掉 /api 前缀后做白名单判断
        String servicePath = path.startsWith("/api/") ? path.substring(4) : path;
        if (WHITE_LIST.stream().anyMatch(servicePath::startsWith) || servicePath.equals("/user")
                || servicePath.equals("/")) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isBlank()) {
            token = exchange.getRequest().getQueryParams().getFirst("token");
        }
        Long userId;
        String role;
        try {
            if (token == null || token.isBlank()) {
                throw new com.aicoding.common.BusinessException(ErrorCode.NOT_LOGIN_ERROR);
            }
            userId = jwtUtils.verifyAndGetUserId(token);
            role = jwtUtils.getRole(token);
        } catch (Exception e) {
            return unauthorized(exchange);
        }
        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Role", role == null ? "user" : role)
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":40100,\"data\":null,\"message\":\"未登录\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
