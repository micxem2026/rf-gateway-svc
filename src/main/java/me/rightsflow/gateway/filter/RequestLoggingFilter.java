package me.rightsflow.gateway.filter;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import me.rightsflow.gateway.service.MetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

@Component
@Slf4j
@RefreshScope
public class RequestLoggingFilter extends AbstractGatewayFilterFactory<RequestLoggingFilter.Config> {

    private final MetricsService metricsService;

    @Value("${rightsflow.gateway.logging.request-headers:true}")
    private boolean logRequestHeaders;
    @Value("${rightsflow.gateway.logging.response-headers:false}")
    private boolean logResponseHeaders;

    public RequestLoggingFilter(MetricsService metricsService) {
        super(Config.class);
        this.metricsService = metricsService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> {
            long startTime = System.currentTimeMillis();
            ServerHttpRequest request = exchange.getRequest();
            HttpHeaders headers = exchange.getRequest().getHeaders();

            metricsService.incrementGatewayRequests(getRouteName(exchange));

            String traceId = MDC.get("traceId");
            String spanId = MDC.get("spanId");

            log.debug("Processing request: {} {} from {}, MDC values - traceId: {}, spanId: {}",
                    request.getMethod(),
                    request.getURI(),
                    getClientIp(request),
                    traceId,
                    spanId);
            if (logRequestHeaders) {
                log.debug("Request headers: {}", headers);
            }

            return chain.filter(exchange)
                    .doFinally(signalType -> logResponse(exchange, startTime));

        }, config.getOrder());
    }

    private void logResponse(ServerWebExchange exchange, long startTime) {
        ServerHttpResponse response = exchange.getResponse();
        HttpHeaders headers = exchange.getResponse().getHeaders();
        long duration = System.currentTimeMillis() - startTime;

        log.debug("Completed response: {} {} - Status: {} - Duration: {}ms",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI(),
                response.getStatusCode(),
                duration);
        if (logResponseHeaders) {
            log.debug("Response headers: {}", headers);
        }
    }

    private String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String getRouteName(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        return route != null ? route.getId() : "unknown";
    }

    @Setter
    @Getter
    public static class Config {
        private int order = 2147483645;
    }

}