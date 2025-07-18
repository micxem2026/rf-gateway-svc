package me.rightsflow.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RefreshScope
public class HealthController implements HealthIndicator {

    private final EurekaDiscoveryClient discoveryClient;
    private final WebClient webClient;

    @Value("${RF_AUTH_SVC_HOSTNAME:localhost}")
    private String authHost;

    @Override
    public Health health() {
        return Health.up()
                .withDetail("gateway", "running")
                .build();
    }

    @GetMapping("/actuator/health/readiness")
    public Mono<ResponseEntity<String>> readiness() {
        return checkEurekaHealth()
                .flatMap(eurekaHealthy -> {
                    if (eurekaHealthy) {
                        return checkAuthServiceHealth();
                    } else {
                        return Mono.just(false);
                    }
                })
                .map(healthy -> {
                    if (healthy) {
                        return ResponseEntity.ok("Ready");
                    } else {
                        return ResponseEntity.status(503).body("Not Ready");
                    }
                });
    }

    @GetMapping("/actuator/health/liveness")
    public Mono<ResponseEntity<String>> liveness() {
        return Mono.just(ResponseEntity.ok("Alive"));
    }

    private Mono<Boolean> checkEurekaHealth() {
        return Mono.fromCallable(() -> {
            try {
                return !discoveryClient.getServices().isEmpty();
            } catch (Exception e) {
                return false;
            }
        });
    }


    private Mono<Boolean> checkAuthServiceHealth() {
        return webClient
                .get()
                .uri("http://%s:9000/actuator/health".formatted(authHost))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> response.contains("UP"))
                .onErrorReturn(false);
    }
}