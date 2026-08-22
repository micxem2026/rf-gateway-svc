package me.rightsflow.gateway.config;

import lombok.RequiredArgsConstructor;
import me.rightsflow.gateway.adapter.InMemoryKeyedRateLimiterAdapter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

/**
 * Per-key rate limiter'ы gateway. Обновление лимитов "на лету" через
 * @RefreshScope на {@link RateLimitProperties} работает автоматически —
 * адаптер читает текущее значение свойства при каждой проверке.
 */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class GatewayRateLimiterConfig {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    private final RateLimitProperties rateLimitProperties;

    @Bean("userRateLimiterBean")
    @Primary
    public RateLimiter<?> userRateLimiter() {
        return new InMemoryKeyedRateLimiterAdapter(rateLimitProperties::getUserLimit, WINDOW);
    }

    @Bean("ipRateLimiterBean")
    public RateLimiter<?> ipRateLimiter() {
        return new InMemoryKeyedRateLimiterAdapter(rateLimitProperties::getIpLimit, WINDOW);
    }

    @Bean("loginRateLimiterBean")
    public RateLimiter<?> loginRateLimiter() {
        return new InMemoryKeyedRateLimiterAdapter(rateLimitProperties::getLoginLimit, WINDOW);
    }
}

