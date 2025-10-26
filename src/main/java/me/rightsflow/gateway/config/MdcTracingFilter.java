package me.rightsflow.gateway.config;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcTracingFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(MdcTracingFilter.class);
    private final Tracer tracer;

    public MdcTracingFilter(Tracer tracer) {
        this.tracer = tracer;
        log.info("MdcTracingFilter initialized with tracer: {}", tracer.getClass().getName());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.debug("=== Before chain - tracer.currentSpan(): {}", tracer.currentSpan());

        return chain.filter(exchange)
                .contextWrite(ctx -> {
                    var span = tracer.currentSpan();
                    log.debug("=== In contextWrite - span: {}", span);

                    if (span != null) {
                        var context = span.context();
                        String traceId = context.traceId();
                        String spanId = context.spanId();

                        log.debug("=== Setting MDC - traceId: {}, spanId: {}", traceId, spanId);
                        MDC.put("traceId", traceId);
                        MDC.put("spanId", spanId);
                    } else {
                        log.warn("=== Span is NULL in contextWrite!");
                    }
                    return ctx;
                })
                .doFinally(signalType -> {
                    log.debug("=== Cleaning MDC");
                    MDC.remove("traceId");
                    MDC.remove("spanId");
                });
    }
}