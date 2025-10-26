package me.rightsflow.gateway.config;

import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

@Configuration(proxyBeanMethods = false)
public class ReactorContextPropagationConfig {

    @PostConstruct
    public void init() {
        // Делает перенос Reactor Context → ThreadLocal «прозрачным» для MDC/Tracer
        Hooks.enableAutomaticContextPropagation();
        // Гарантируем регистрацию аксессора для Micrometer Observation/Tracing
        ContextRegistry.getInstance().registerThreadLocalAccessor(new ObservationThreadLocalAccessor());
    }

}