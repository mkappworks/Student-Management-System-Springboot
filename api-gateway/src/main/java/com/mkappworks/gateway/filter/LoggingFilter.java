package com.mkappworks.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.time.Instant;

@Component
@Slf4j
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var req = exchange.getRequest();
        long start = Instant.now().toEpochMilli();
        log.info("Request: {} {}", req.getMethod(), req.getURI().getPath());
        return chain.filter(exchange).then(Mono.fromRunnable(() ->
                log.info("Response: {} - {}ms", exchange.getResponse().getStatusCode(),
                        Instant.now().toEpochMilli() - start)));
    }

    @Override
    public int getOrder() { return -2; }
}
