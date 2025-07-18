package me.rightsflow.gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.dto.ErrorResponse;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;

@Component
@Order(-2)
@Slf4j
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        // Настраиваем форматированный вывод
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        return createErrorResponse(exchange, ex)
                .flatMap(errorResponse -> {
                    if (ex instanceof AuthenticationException) {
                        log.warn("Authentication failed for URI: {} - {}", exchange.getRequest().getURI(), ex.getMessage());
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    } else if (ex instanceof AccessDeniedException) {
                        log.warn("Access denied for URI: {} - {}", exchange.getRequest().getURI(), ex.getMessage());
                        response.setStatusCode(HttpStatus.FORBIDDEN);
                    } else {
                        log.error("Unexpected error for URI: {} - {}", exchange.getRequest().getURI(), ex.getMessage(), ex);
                        response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                    try {
                        String body = objectMapper.writeValueAsString(errorResponse);
                        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                        return response.writeWith(Mono.just(buffer));
                    } catch (Exception e) {
                        log.error("Error serializing error response", e);
                        return Mono.error(e);
                    }
                });
    }

    private Mono<ErrorResponse> createErrorResponse(ServerWebExchange exchange, Throwable ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;

        if (ex instanceof AuthenticationException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex instanceof AccessDeniedException) {
            status = HttpStatus.FORBIDDEN;
        }

        final HttpStatus finalStatus = status;

        return getPrincipal(exchange)
                .map(principal -> ErrorResponse.builder()
                        .uri(exchange.getRequest().getURI().toString())
                        .principal(principal)
                        .status(finalStatus.value())
                        .error(ex.getMessage())
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    private Mono<String> getPrincipal(ServerWebExchange exchange) {
        return exchange.getPrincipal()
                .cast(Authentication.class)
                .map(Principal::getName)
                .defaultIfEmpty("anonymous"); // или другое значение по умолчанию
    }
}