package me.rightsflow.gateway.config;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MetricsConfig {

    @Bean
    public MeterFilter meterFilter() {
        return MeterFilter.deny(id -> {
            String name = id.getName();
            // Фильтруем только нужные метрики
            return !name.startsWith("http_server_requests_seconds") &&
                    !name.startsWith("jvm_memory") &&
                    !name.startsWith("jvm_threads") &&
                    !name.startsWith("jvm_gc_pause_seconds") &&
                    !name.startsWith("reactor_netty") &&
                    !name.startsWith("resilience4j.ratelimiter") &&
                    !name.startsWith("authenticated_requests_total") &&
                    !name.startsWith("unauthenticated_requests_total") &&
                    !name.startsWith("gateway.");
        });
    }
}