package me.rightsflow.gateway.service;

import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@Slf4j
@RefreshScope
public class FallbackService {

    @Value("${rightsflow.gateway.fallback.message:Сервис временно недоступен}")
    private String fallbackMessage;

    public Mono<ErrorResponse> createFallbackResponse(ServerWebExchange exchange) {
        log.warn("Fallback triggered for URI: {}", exchange.getRequest().getURI());

        return Mono.just(ErrorResponse.builder()
                .uri(exchange.getRequest().getURI().toString())
                .principal(null)
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error(fallbackMessage)
                .timestamp(LocalDateTime.now())
                .build());
    }
}