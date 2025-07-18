package me.rightsflow.gateway.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void incrementAuthenticatedRequests() {
        Counter.builder("authenticated_requests_total")
                .description("Total number of authenticated requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementUnauthenticatedRequests() {
        Counter.builder("unauthenticated_requests_total")
                .description("Total number of unauthenticated requests")
                .register(meterRegistry)
                .increment();
    }

    public void incrementGatewayRequests(String route) {
        Counter.builder("gateway.requests")
                .description("Total number of gateway requests")
                .tag("route", route)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRouteMatched(String route) {
        Counter.builder("gateway.routes.matched")
                .description("Total number of matched routes")
                .tag("route", route)
                .register(meterRegistry)
                .increment();
    }

    public void incrementRouteUnmatched() {
        Counter.builder("gateway.routes.unmatched")
                .description("Total number of unmatched routes")
                .register(meterRegistry)
                .increment();
    }

    public Timer.Sample startFilterTimer(String filterName) {
        return Timer.start(meterRegistry);
    }

    public void recordFilterLatency(Timer.Sample sample, String filterName) {
        sample.stop(Timer.builder("gateway.filter.latency")
                .description("Filter execution latency")
                .tag("filter", filterName)
                .register(meterRegistry));
    }

    public void incrementFilterInvocations(String filterName) {
        Counter.builder("gateway.filter.invocations")
                .description("Total number of filter invocations")
                .tag("filter", filterName)
                .register(meterRegistry)
                .increment();
    }
}

