package me.rightsflow.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "rightsflow.gateway.rate-limiting")
@RefreshScope
@Component
@Data
public class RateLimitProperties {
    /** Лимит запросов в минуту на одного аутентифицированного пользователя (или "anonymous"). */
    private int userLimit = 100;
    /** Лимит запросов в минуту на один IP. */
    private int ipLimit = 100;
    /** Лимит попыток входа в минуту на один IP (POST /auth/api/auth/v1/login). */
    private int loginLimit = 10;
    // timeoutDuration убран — новый адаптер не блокирует поток, а сразу отвечает allow/deny
}