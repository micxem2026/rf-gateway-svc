package me.rightsflow.gateway.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CacheManager cacheManager;

    @PostMapping("/cache/jwks/clear")
    public Mono<ResponseEntity<String>> clearJwksCache() {
        return Mono.fromRunnable(() -> {
            var cache = cacheManager.getCache("jwks-cache");
            if (cache != null) {
                cache.clear();
            }
        }).then(Mono.just(ResponseEntity.ok("JWKS cache cleared successfully")));
    }
}