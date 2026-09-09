package com.devcart.apigateway.filter;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.devcart.apigateway.config.JwtProperties;

import reactor.core.publisher.Mono;

/**
 * Filtro global que executa apos a validacao do JWT pelo Spring Security e:
 * <ol>
 *   <li>remove headers de identidade eventualmente forjados pelo cliente
 *       ({@code X-User-Id}, {@code X-User-Roles});</li>
 *   <li>extrai as claims do token autenticado e injeta {@code X-User-Id}
 *       (e {@code X-User-Roles}) em TODA requisicao repassada aos microsservicos.</li>
 * </ol>
 * Os servicos a jusante nao precisam reprocessar o JWT: confiam no header
 * assinado pelo perimetro (o gateway).
 */
@Component
public class JwtClaimsPropagationGlobalFilter implements GlobalFilter, Ordered {

    public static final String X_USER_ID = "X-User-Id";
    public static final String X_USER_ROLES = "X-User-Roles";

    /**
     * Executa cedo na cadeia do gateway. O SecurityContext ja esta disponivel
     * porque o WebFilter do Spring Security envolve todo o handler do gateway.
     */
    private static final int ORDER = Ordered.HIGHEST_PRECEDENCE + 100;

    private final JwtProperties jwtProperties;

    public JwtClaimsPropagationGlobalFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 1. Anti-spoofing: descarta qualquer header de identidade vindo do cliente.
        ServerWebExchange sanitized = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.remove(X_USER_ID);
                            headers.remove(X_USER_ROLES);
                        })
                        .build())
                .build();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(authentication -> propagate(sanitized, chain, authentication))
                .switchIfEmpty(Mono.defer(() -> unauthorized(sanitized, "Token JWT ausente ou invalido")));
    }

    private Mono<Void> propagate(ServerWebExchange exchange, GatewayFilterChain chain, JwtAuthenticationToken authentication) {
        Jwt jwt = authentication.getToken();
        String userId = jwt.getClaimAsString(jwtProperties.getUserIdClaim());

        if (userId == null || userId.isBlank()) {
            return unauthorized(exchange, "JWT sem a claim '" + jwtProperties.getUserIdClaim() + "'");
        }

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        ServerHttpRequest mutated = exchange.getRequest().mutate()
                .header(X_USER_ID, userId)
                .header(X_USER_ROLES, roles)
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"unauthorized\",\"message\":\"" + message + "\"}")
                .getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
