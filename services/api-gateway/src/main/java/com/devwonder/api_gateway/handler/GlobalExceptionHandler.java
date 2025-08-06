package com.devwonder.api_gateway.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
@Order(-1)
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        
        HttpStatus status;
        String errorMessage;
        String errorType;
        
        if (ex instanceof ResponseStatusException) {
            ResponseStatusException rse = (ResponseStatusException) ex;
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            errorMessage = rse.getReason() != null ? rse.getReason() : status.getReasonPhrase();
            errorType = "ResponseStatusException";
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorMessage = "Internal server error";
            errorType = "InternalServerError";
            log.error("Internal server error for {} {} - RequestId: {}", method, path, requestId, ex);
        }
        
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        
        String body = String.format(
            "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\",\"method\":\"%s\",\"requestId\":\"%s\",\"type\":\"%s\"}", 
            Instant.now(),
            status.value(),
            status.getReasonPhrase(),
            errorMessage,
            path,
            method,
            requestId != null ? requestId : "unknown",
            errorType
        );
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}