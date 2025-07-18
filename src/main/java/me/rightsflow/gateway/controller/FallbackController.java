package me.rightsflow.gateway.controller;

import lombok.RequiredArgsConstructor;
import me.rightsflow.gateway.dto.ErrorResponse;
import me.rightsflow.gateway.service.FallbackService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FallbackController {

    private final FallbackService fallbackService;

    @RequestMapping("/fallback")
    public Mono<ResponseEntity<ErrorResponse>> fallback(ServerWebExchange exchange) {
        return fallbackService.createFallbackResponse(exchange)
                .map(errorResponse -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse));
    }
}