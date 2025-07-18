package me.rightsflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@Slf4j
@RefreshScope
public class RedirectRewriteFilter extends AbstractGatewayFilterFactory<RedirectRewriteFilter.Config> {

    @Value("${server.port:8090}")
    private int gatewayPort;

    @Value("${RF_GATEWAY_SVC_HOSTNAME:${hostname:localhost}}")
    private String gatewayHost;


    public RedirectRewriteFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                ServerHttpResponse response = exchange.getResponse();

                // Проверяем, является ли ответ редиректом
                if (isRedirect(response.getStatusCode())) {
                    String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);

                    if (location != null) {
                        String rewrittenLocation = rewriteRedirectLocation(location, exchange);

                        if (!location.equals(rewrittenLocation)) {
                            log.info("Rewriting redirect from {} to {}", location, rewrittenLocation);
                            response.getHeaders().set(HttpHeaders.LOCATION, rewrittenLocation);
                        }
                    }
                }
            }));
        };
    }

    private boolean isRedirect(HttpStatusCode status) {
        return (
                status == HttpStatus.MOVED_PERMANENTLY ||
                        status == HttpStatus.FOUND ||
                        status == HttpStatus.SEE_OTHER ||
                        status == HttpStatus.TEMPORARY_REDIRECT ||
                        status == HttpStatus.PERMANENT_REDIRECT
        );
    }

    private String rewriteRedirectLocation(String originalLocation, ServerWebExchange exchange) {
        try {
            log.debug("Original redirect location: {}", originalLocation);
            URI originalUri = URI.create(originalLocation);
            //String host = originalUri.getHost();
            //int port = originalUri.getPort();

            // Проверяем, нужно ли перенаправлять через Gateway
            //String serviceAddress = host + ":" + port;

            //if (proxiedServices.contains(serviceAddress)) {
                // Перезаписываем URL для прохождения через Gateway
                String gatewayUrl = "http://" + gatewayHost +  ":" + gatewayPort;
                String path = originalUri.getPath();
                String query = originalUri.getQuery();

                StringBuilder rewrittenUrl = new StringBuilder(gatewayUrl);

                if (path != null) {
                    rewrittenUrl.append(path);
                }

                if (query != null) {
                    rewrittenUrl.append("?").append(query);
                }

                return rewrittenUrl.toString();
            //}

            // Если это внешний редирект или не проксируемый сервис, оставляем как есть
            //return originalLocation;

        } catch (Exception e) {
            log.warn("Failed to rewrite redirect location: {}", originalLocation, e);
            return originalLocation;
        }
    }

    public static class Config {
        // Configuration properties if needed
    }
}