package me.rightsflow.gateway.adapter;


import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

@Slf4j
public class CachedReactiveJwtDecoder implements ReactiveJwtDecoder {

    private final ReactiveJwtDecoder delegate;
    private final Cache cache;

    public CachedReactiveJwtDecoder(ReactiveJwtDecoder delegate, CacheManager cacheManager) {
        this.delegate = delegate;
        this.cache = cacheManager.getCache("jwks-cache");
    }

    @Override
    public Mono<Jwt> decode(String token) throws JwtException {
        return Mono.fromCallable(() -> {
                    Cache.ValueWrapper cached = cache.get(token);
                    if (cached != null) log.debug("Using cached JWT for token: {}", token);
                    return cached != null ? (Jwt) cached.get() : null;
                })
                .switchIfEmpty(
                        delegate.decode(token)
                                .doOnNext(jwt -> {
                                    log.debug("Caching JWT for token: {}", token);
                                    cache.put(token, jwt);
                                })
                );
    }
}