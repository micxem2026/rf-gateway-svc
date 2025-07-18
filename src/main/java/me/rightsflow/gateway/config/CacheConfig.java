package me.rightsflow.gateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@RefreshScope
public class CacheConfig {

    @Value("${rightsflow.gateway.jwks.cache-duration:3600s}")
    private Duration jwksCacheDuration;

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> cacheManagerCustomizer() {
        return cacheManager -> {
            cacheManager.setCacheNames(List.of("jwks-cache"));
            cacheManager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(100)
                    .expireAfterWrite(jwksCacheDuration)
                    .recordStats());
        };
    }
}