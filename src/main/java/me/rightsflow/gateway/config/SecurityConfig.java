package me.rightsflow.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.adapter.CachedReactiveJwtDecoder;
import me.rightsflow.gateway.dto.ErrorResponse;
import me.rightsflow.gateway.security.JwtAuthenticationConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwksUri;

    private final WebClient webClient;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final CacheManager cacheManager;
    private final PublicPathsProperties publicPathsProperties;
    private final ObjectMapper objectMapper;

    @Bean
    @RefreshScope
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                publicPathsProperties.getPublicPaths().toArray(new String[0])
                        ).permitAll()
                        .anyExchange().authenticated()
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((webFilterChain, ex) -> {
                            ServerHttpResponse response = webFilterChain.getResponse();
                            if (response.isCommitted()) {
                                return Mono.error(ex);
                            }

                            return Mono.just("ananonymous")
                                    .map(principal -> ErrorResponse.builder()
                                            .uri(webFilterChain.getRequest().getURI().toString())
                                            .principal(principal)
                                            .status(HttpStatus.UNAUTHORIZED.value())
                                            .error(ex.getMessage())
                                            .timestamp(LocalDateTime.now())
                                            .build())
                                    .flatMap(errorResponse -> {
                                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                                        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                                        try {
                                            String body = objectMapper.writeValueAsString(errorResponse);
                                            DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                                            return response.writeWith(Mono.just(buffer));
                                        } catch (Exception e) {
                                            log.error("Error serializing error response", e);
                                            return Mono.error(e);
                                        }
                                    });
                        })
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter)
                        )
                )
                .build();
    }

    @Bean
    @RefreshScope
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        ReactiveJwtDecoder delegate = NimbusReactiveJwtDecoder
                .withJwkSetUri(jwksUri)
                .webClient(webClient)
                .build();

        return new CachedReactiveJwtDecoder(delegate, cacheManager);
    }

}
