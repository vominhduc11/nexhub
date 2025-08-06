package com.devwonder.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.springframework.security.core.context.SecurityContext;

@Slf4j
@Component
public class JwtClaimsGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Add request ID for tracing
        String requestId = java.util.UUID.randomUUID().toString();
        
        return ReactiveSecurityContextHolder.getContext()
            .cast(SecurityContext.class)
            .map(securityContext -> securityContext.getAuthentication())
            .cast(JwtAuthenticationToken.class)
            .map(jwtAuthenticationToken -> {
                try {
                    Jwt jwt = jwtAuthenticationToken.getToken();
                    
                    // Extract claims from JWT with null safety
                    String username = jwt.getClaimAsString("sub");
                    String role = jwt.getClaimAsString("role");
                    String issuer = jwt.getClaimAsString("iss");
                    
                    log.debug("Processing JWT for user: {} with role: {} from issuer: {}", 
                               username, role, issuer);
                    
                    // Validate issuer
                    if (!"nexhub-auth-service".equals(issuer)) {
                        log.warn("Invalid JWT issuer: {} for user: {}", issuer, username);
                    }
                    
                    // Add JWT claims to headers for downstream services
                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header(USER_ID_HEADER, username != null ? username : "")
                        .header(USER_ROLE_HEADER, role != null ? role : "")
                        .header(REQUEST_ID_HEADER, requestId)
                        .build();
                    
                    return exchange.mutate().request(modifiedRequest).build();
                    
                } catch (Exception e) {
                    log.warn("Error processing JWT claims: {}", e.getMessage());
                    // Return original exchange if JWT processing fails
                    return exchange;
                }
            })
            .defaultIfEmpty(addRequestIdToExchange(exchange, requestId))
            .flatMap(modifiedExchange -> chain.filter(modifiedExchange));
    }
    private ServerWebExchange addRequestIdToExchange(ServerWebExchange exchange, String requestId) {
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
            .header(REQUEST_ID_HEADER, requestId)
            .build();
        return exchange.mutate().request(modifiedRequest).build();
    }

    @Override
    public int getOrder() {
        return -100; // Execute after authentication but before routing
    }
}