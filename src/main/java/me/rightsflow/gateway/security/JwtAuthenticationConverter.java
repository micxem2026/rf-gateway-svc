package me.rightsflow.gateway.security;

import lombok.extern.slf4j.Slf4j;
import me.rightsflow.gateway.dto.UserContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class JwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(@NonNull Jwt jwt) {
        try {
            log.debug("Converting JWT to authentication token for subject: {}", jwt.getSubject());
            UserContext userContext = UserContext.builder()
                    .userId(jwt.getClaimAsString("user_id"))
                    .roles(jwt.getClaimAsStringList("roles"))
                    .userType(jwt.getClaimAsString("user_type"))
                    .clientId(jwt.getClaimAsString("client_id"))
                    .scopes(jwt.getClaimAsStringList("scope"))
                    .subject(jwt.getSubject())
                    .build();
            log.debug("User context: {}", userContext);
            return Mono.just(new CustomUserPrincipal(jwt, userContext));
        } catch (Exception e) {
            log.error("Failed to convert JWT: {}", e.getMessage());
            return Mono.empty();
        }
    }
}