package me.rightsflow.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.dto.UserContext;
import me.rightsflow.gateway.security.CustomUserPrincipal;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class UserContextFilter extends AbstractGatewayFilterFactory<UserContextFilter.Config> {

    public UserContextFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> ReactiveSecurityContextHolder.getContext()
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("No SecurityContext found, proceeding without headers");
                    return Mono.just(new SecurityContextImpl()); // Пустой SecurityContext
                }))
                .flatMap(securityContext -> {
                    Authentication authentication = securityContext.getAuthentication();
                    if (!(authentication instanceof CustomUserPrincipal principal)) {
                        log.debug("Authentication is not CustomUserPrincipal, proceeding without headers");
                        return chain.filter(exchange);
                    }
                    UserContext userContext = principal.getUserContext();
                    ServerWebExchange modifiedExchange = addUserHeaders(exchange, userContext);
                    return chain.filter(modifiedExchange);
                });
    }

    private ServerWebExchange addUserHeaders(ServerWebExchange exchange, UserContext userContext) {
        ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate();

        if (userContext.getSubject() != null) {
            requestBuilder.header("X-Subject", userContext.getSubject());
        }

        if (userContext.getUserId() != null) {
            requestBuilder.header("X-User-Id", userContext.getUserId());
        }

        if (userContext.getRoles() != null && !userContext.getRoles().isEmpty()) {
            requestBuilder.header("X-User-Roles", String.join(",", userContext.getRoles()));
        }

        if (userContext.getUserType() != null) {
            requestBuilder.header("X-User-Type", userContext.getUserType());
        }

        if (userContext.getClientId() != null) {
            requestBuilder.header("X-Client-Id", userContext.getClientId());
        }

        if (userContext.getScopes() != null && !userContext.getScopes().isEmpty()) {
            requestBuilder.header("X-Scopes", String.join(",", userContext.getScopes()));
        }

        String logId = userContext.getSubject() != null ? userContext.getSubject() :
                userContext.getClientId() != null ? userContext.getClientId() : "unknown";
        log.debug("Added user context headers for user: {}", logId);

        return exchange.mutate().request(requestBuilder.build()).build();
    }

    public static class Config {
        // Configuration properties if needed
    }
}