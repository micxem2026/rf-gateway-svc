package me.rightsflow.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final ReactiveJwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;


    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        return Mono.justOrEmpty(authentication)
                .cast(BearerTokenAuthenticationToken.class)
                .flatMap(token -> jwtDecoder.decode(token.getToken())
                        .onErrorResume(e -> {
                            log.warn("JWT decoding failed: {}", e.getMessage());
                            return Mono.empty();
                        }))
                .flatMap(jwt -> Objects.requireNonNull(jwtAuthenticationConverter.convert(jwt))
                        .filter(Objects::nonNull)
                        .cast(Authentication.class)
                        .switchIfEmpty(Mono.defer(() -> {
                            log.warn("JwtAuthenticationConverter returned empty Mono or null for token");
                            return Mono.empty();
                        })))
                .doOnSuccess(auth -> log.debug("Successfully authenticated user: {}", auth.getName()))
                .doOnError(error -> log.warn("Authentication failed: {}", error.getMessage()));
    }
}

