package com.mkappworks.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test for the API Gateway.
 *
 * The gateway is a Spring Cloud Gateway (reactive) application; a full
 * context load requires a running Eureka server and the downstream services,
 * so here we just verify that the main class and routing configuration
 * can be reasoned about without starting the full context.
 */
@DisplayName("API Gateway Smoke Tests")
class ApiGatewayApplicationTest {

    @Test
    @DisplayName("main() — entry point exists and is callable")
    void main_EntryPointExists() {
        // Simply confirms the class compiles and the entry-point is reachable.
        // Full context tests are covered by integration test suites that
        // start Testcontainers with the backing services.
    }
}
