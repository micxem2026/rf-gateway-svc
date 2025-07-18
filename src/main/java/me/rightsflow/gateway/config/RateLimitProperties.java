package me.rightsflow.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.time.Duration;

@ConfigurationProperties(prefix = "rightsflow.gateway.rate-limiting")
@RefreshScope
@Component
@Data
public class RateLimitProperties {
    private int userLimit = 100;
    private int ipLimit = 100;
    private Duration timeoutDuration = Duration.ofSeconds(1);
}
