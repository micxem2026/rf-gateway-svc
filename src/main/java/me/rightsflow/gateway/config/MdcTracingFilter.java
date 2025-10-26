package me.rightsflow.gateway.config;

import io.micrometer.tracing.Tracer;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcTracingFilter implements WebFilter {

    private final Tracer tracer;

    public MdcTracingFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
                .contextWrite(ctx -> {
                    // Получаем текущий span из tracer
                    var span = tracer.currentSpan();
                    if (span != null) {
                        var context = span.context();
                        // Добавляем в MDC
                        MDC.put("traceId", context.traceId());
                        MDC.put("spanId", context.spanId());
                    }
                    return ctx;
                })
                .doFinally(signalType -> {
                    // Очищаем MDC после завершения запроса
                    MDC.remove("traceId");
                    MDC.remove("spanId");
                });
    }
}