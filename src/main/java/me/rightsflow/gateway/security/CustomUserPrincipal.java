package me.rightsflow.gateway.security;

import lombok.Getter;
import me.rightsflow.gateway.dto.UserContext;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Getter
public class CustomUserPrincipal extends JwtAuthenticationToken {

    private final UserContext userContext;

    public CustomUserPrincipal(Jwt jwt, UserContext userContext) {
        super(jwt, createAuthorities(userContext));
        this.userContext = userContext;
    }

    private static Collection<GrantedAuthority> createAuthorities(UserContext userContext) {
        List<GrantedAuthority> authorities =
                userContext.getRoles() != null
                        ? userContext.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList())
                        : List.of();

        return userContext.getScopes() != null
                ? Stream.concat(authorities.stream(), userContext.getScopes().stream()
                .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))).toList()
                : authorities;
    }

}