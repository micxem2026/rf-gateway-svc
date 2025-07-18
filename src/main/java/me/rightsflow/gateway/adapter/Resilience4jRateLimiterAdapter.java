package me.rightsflow.gateway.adapter;

import io.github.resilience4j.ratelimiter.RateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Map;

@RequiredArgsConstructor
@Slf4j
public class Resilience4jRateLimiterAdapter implements org.springframework.cloud.gateway.filter.ratelimit.RateLimiter<Object> {

    private final RateLimiter rateLimiter;

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return Mono.fromCallable(() -> {
                    try {
                        boolean allowed = rateLimiter.acquirePermission(1);

                        if (allowed) {
                            log.debug("Rate limit check passed for route: {}, id: {}", routeId, id);
                            return new Response(true, Map.of(
                                    "allowed", "true",
                                    "remaining", Integer.toString(rateLimiter.getMetrics().getAvailablePermissions())
                            ));
                        } else {
                            log.warn("Rate limit exceeded for route: {}, id: {}", routeId, id);
                            return new Response(false, Map.of(
                                    "allowed", "false",
                                    "remaining", "0"
                            ));
                        }
                    } catch (Exception e) {
                        log.error("Error checking rate limit for route: {}, id: {}", routeId, id, e);
                        // В случае ошибки разрешаем запрос
                        return new Response(true, Map.of("allowed", "true", "error", "true"));
                    }
                })
                .onErrorReturn(new Response(true, Map.of("allowed", "true", "error", "true")));
    }

    @Override
    public Map<String, Object> getConfig() {
        return Map.of(
                "name", rateLimiter.getName(),
                "limitForPeriod", String.valueOf(rateLimiter.getRateLimiterConfig().getLimitForPeriod()),
                "limitRefreshPeriod", rateLimiter.getRateLimiterConfig().getLimitRefreshPeriod().toString()
        );
    }

    @Override
    public Class<Object> getConfigClass() {
        return Object.class;
    }

    @Override
    public Object newConfig() {
        return new Object();
    }
}
