package me.rightsflow.gateway.listener;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.config.RateLimitProperties;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimiterUpdater implements ApplicationListener<RefreshScopeRefreshedEvent> {

    private final RateLimiterRegistry rateLimiterRegistry;
    private final RateLimitProperties rateLimitProperties;

    @Override
    public void onApplicationEvent(@NonNull RefreshScopeRefreshedEvent event) {
        log.info("Refresh event received. Updating rate limiters...");
        updateUserRateLimiter();
        updateIpRateLimiter();
    }

    private void updateUserRateLimiter() {
        rateLimiterRegistry.find("user-rate-limiter").ifPresent(rateLimiter -> {
            int newLimit = rateLimitProperties.getUserLimit();
            Duration newTimeout = rateLimitProperties.getTimeoutDuration();
            log.info("Updating user-rate-limiter. New limit: {}, new timeout duration: {}", newLimit, newTimeout);
            rateLimiter.changeLimitForPeriod(newLimit);
            rateLimiter.changeTimeoutDuration(newTimeout);
        });
    }

    private void updateIpRateLimiter() {
        rateLimiterRegistry.find("ip-rate-limiter").ifPresent(rateLimiter -> {
            int newLimit = rateLimitProperties.getIpLimit();
            Duration newTimeout = rateLimitProperties.getTimeoutDuration();
            log.info("Updating ip-rate-limiter. New limit: {}, new timeout duration: {}", newLimit, newTimeout);
            rateLimiter.changeLimitForPeriod(newLimit);
            rateLimiter.changeTimeoutDuration(newTimeout);
        });
    }


}