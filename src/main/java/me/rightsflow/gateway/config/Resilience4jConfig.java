package me.rightsflow.gateway.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import me.rightsflow.gateway.adapter.Resilience4jRateLimiterAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
public class Resilience4jConfig {

    private final RateLimitProperties rateLimitProperties;

    @Bean
    @Primary
    public RateLimiterRegistry rateLimiterRegistry() {
        return RateLimiterRegistry.ofDefaults();
    }

    @Bean("userRateLimiterBean")
    @Primary
    public org.springframework.cloud.gateway.filter.ratelimit.RateLimiter<?> userRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(rateLimitProperties.getUserLimit())
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(rateLimitProperties.getTimeoutDuration())
                .build();

        RateLimiter rateLimiter = registry.rateLimiter("user-rate-limiter", config);
        return new Resilience4jRateLimiterAdapter(rateLimiter);
    }

    @Bean("ipRateLimiterBean")
    public org.springframework.cloud.gateway.filter.ratelimit.RateLimiter<?> ipRateLimiter(RateLimiterRegistry registry) {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(rateLimitProperties.getIpLimit())
                .limitRefreshPeriod(Duration.ofSeconds(60))
                .timeoutDuration(rateLimitProperties.getTimeoutDuration())
                .build();

        RateLimiter rateLimiter = registry.rateLimiter("ip-rate-limiter", config);
        return new Resilience4jRateLimiterAdapter(rateLimiter);
    }
}