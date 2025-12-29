package me.rightsflow.gateway.filter;

import lombok.Getter;
import lombok.Setter;
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
import org.springframework.web.util.UriComponentsBuilder;
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
                        String rewrittenLocation = rewriteRedirectLocation(location, exchange, config);

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

    private String rewriteRedirectLocation(String originalLocation, ServerWebExchange exchange, Config config) {
        try {
            log.debug("Original redirect location: {}", originalLocation);
            URI originalUri = URI.create(originalLocation);

            String redirectHostUri;
            String kubernetesServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");
            if (kubernetesServiceHost != null && !kubernetesServiceHost.isEmpty()) {
                // Сервис запущен в Kubernetes
                log.debug("Running in Kubernetes");
                redirectHostUri = config.getProtocol() + "://" + config.getRedirectHost() +  ":" + config.getRedirectPort();
            } else {
                // Сервис не запущен в Kubernetes
                log.debug("Not running in Kubernetes");
                redirectHostUri = "http://" + gatewayHost +  ":" + gatewayPort;
            }

            // Используем UriComponentsBuilder для правильной пересборки URL
            UriComponentsBuilder builder = UriComponentsBuilder.fromUri(originalUri);

            // Заменяем хост и порт на наши
            builder.host(URI.create(redirectHostUri).getHost())
                    .port(URI.create(redirectHostUri).getPort())
                    .scheme(URI.create(redirectHostUri).getScheme());

            return builder.build(true).toUriString();

         } catch (Exception e) {
            log.warn("Failed to rewrite redirect location: {}", originalLocation, e);
            return originalLocation;
        }
    }

    @Getter
    @Setter
    public static class Config {
        private String protocol;
        private String redirectHost;
        private String redirectPort;
    }
}