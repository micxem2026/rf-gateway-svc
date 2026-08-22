package me.rightsflow.gateway.adapter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;

/**
 * Per-key rate limiter: у каждого уникального ключа (username, IP) — своё
 * независимое окно
 * <p>
 * Ограничение: состояние in-memory, не распределено между репликами gateway.
 * При масштабировании на несколько подов нужен Redis-backed лимитер.
 */
@Slf4j
public class InMemoryKeyedRateLimiterAdapter implements RateLimiter<Object> {

    private final IntSupplier limitForPeriod;
    private final Duration windowDuration;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public InMemoryKeyedRateLimiterAdapter(IntSupplier limitForPeriod, Duration windowDuration) {
        this.limitForPeriod = limitForPeriod;
        this.windowDuration = windowDuration;
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        return Mono.fromCallable(() -> {
                    Window window = windows.computeIfAbsent(id, k -> new Window());
                    int limit = limitForPeriod.getAsInt();
                    int count = window.recordAttempt(windowDuration.toMillis());
                    boolean allowed = count <= limit;

                    if (!allowed) {
                        log.warn("Keyed rate limit exceeded for route: {}, key: {} ({} attempts)",
                                routeId, id, count);
                    }
                    return new Response(allowed, Map.of(
                            "allowed", String.valueOf(allowed),
                            "limit", String.valueOf(limit)));
                })
                .onErrorReturn(new Response(true, Map.of("allowed", "true", "error", "true")));
    }

    @Override
    public Map<String, Object> getConfig() {
        return Map.of("limitForPeriod", limitForPeriod.getAsInt(), "window", windowDuration.toString());
    }

    @Override
    public Class<Object> getConfigClass() {
        return Object.class;
    }

    @Override
    public Object newConfig() {
        return new Object();
    }

    /** Периодическая очистка устаревших окон — иначе карта растёт бесконечно. */
    @Scheduled(fixedDelay = 600_000) // каждые 10 минут
    public void cleanupExpiredWindows() {
        long now = System.currentTimeMillis();
        long maxAgeMillis = windowDuration.toMillis() * 2;
        int before = windows.size();
        windows.entrySet().removeIf(e -> now - e.getValue().getWindowStart() > maxAgeMillis);
        int removed = before - windows.size();
        if (removed > 0) {
            log.debug("Cleaned up {} expired rate-limit windows", removed);
        }
    }

    private static class Window {
        private volatile long windowStart = System.currentTimeMillis();
        private int count = 0;

        synchronized int recordAttempt(long windowMillis) {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMillis) {
                windowStart = now;
                count = 0;
            }
            return ++count;
        }

        long getWindowStart() {
            return windowStart;
        }
    }
}
