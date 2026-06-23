package com.example.ratelimit.service;

import com.example.ratelimit.config.ConfigLoader;
import com.example.ratelimit.config.RateLimitConfig;
import com.example.ratelimit.config.RateLimitRule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

    private RateLimiterService service;

    private static RateLimitRule rule(String window, int max) {
        return new RateLimitRule(window, max);
    }

    private static RateLimitConfig config(List<String> admins, List<RateLimitRule> defaults, Map<String, List<RateLimitRule>> clients) {
        return new RateLimitConfig(admins, defaults, clients);
    }

    private RateLimiterService buildService(RateLimitConfig cfg) {
        var srv = new RateLimiterService();
        srv.configLoader = new ConfigLoader() {
            @Override
            public RateLimitConfig getConfig() { return cfg; }
        };
        srv.slidingWindowLog = new SlidingWindowLog();
        return srv;
    }

    private void assertAllowed(String clientId, int times) {
        for (int i = 0; i < times; i++) {
            var result = service.checkRateLimit(clientId);
            assertThat(result.isAllowed())
                .as("request %d for %s should be allowed", i + 1, clientId)
                .isTrue();
        }
    }

    private void assertDenied(String clientId, String expectedMessage) {
        var result = service.checkRateLimit(clientId);
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReason()).isEqualTo(expectedMessage);
    }

    private void assertDeniedOnNth(String clientId, int n, String expectedMessage) {
        assertAllowed(clientId, n - 1);
        assertDenied(clientId, expectedMessage);
    }

    @Test
    @DisplayName("recognizes admin client IDs")
    void adminBypass() {
        service = buildService(config(List.of("admin-1"), List.of(rule("1m", 2)), Map.of()));
        assertThat(service.isAdmin("admin-1")).isTrue();
        assertThat(service.isAdmin("unknown")).isFalse();
    }

    @Test
    @DisplayName("allows requests under the threshold")
    void withinLimit() {
        service = buildService(config(List.of(), List.of(rule("1m", 3)), Map.of()));
        assertAllowed("alice", 3);
    }

    @Test
    @DisplayName("denies request that exceeds the threshold")
    void exceedingLimit() {
        service = buildService(config(List.of(), List.of(rule("1m", 3)), Map.of()));
        assertDeniedOnNth("bob", 4, "exceed 3 requests for last 1m");
    }

    @Test
    @DisplayName("uses client-specific rules when configured")
    void clientSpecificRules() {
        service = buildService(config(List.of(), List.of(rule("1m", 3)), Map.of("vip", List.of(rule("1m", 5)))));
        assertDeniedOnNth("vip", 6, "exceed 5 requests for last 1m");
    }

    @Test
    @DisplayName("re-allows request after the window elapses")
    void reallowAfterWindow() throws InterruptedException {
        service = buildService(config(List.of(), List.of(rule("1s", 2)), Map.of()));
        assertAllowed("charlie", 2);
        assertDenied("charlie", "exceed 2 requests for last 1s");
        Thread.sleep(1100);
        assertAllowed("charlie", 1);
    }

    @Test
    @DisplayName("different clients do not interfere with each other")
    void clientIsolation() {
        service = buildService(config(List.of(), List.of(rule("1m", 3)), Map.of()));
        assertDeniedOnNth("alice", 4, "exceed 3 requests for last 1m");
        assertAllowed("bob", 3);
    }

    @Test
    @DisplayName("denies when any rule threshold is breached (multiple rules)")
    void multipleRules() {
        service = buildService(config(List.of(), List.of(rule("1m", 3), rule("10m", 5)), Map.of()));
        assertDeniedOnNth("multi", 4, "exceed 3 requests for last 1m");
    }

    @Test
    @DisplayName("allows client with no matching rules (no default, no client-specific)")
    void noRulesForClient() {
        service = buildService(config(List.of(), List.of(), Map.of()));
        assertAllowed("nobody", 10);
    }

    @Test
    @DisplayName("default rules apply when client has no specific rules")
    void fallsBackToDefault() {
        service = buildService(config(List.of(), List.of(rule("1m", 2)), Map.of("vip", List.of(rule("1m", 5)))));
        assertDeniedOnNth("guest", 3, "exceed 2 requests for last 1m");
    }

    @Test
    @DisplayName("handles concurrent requests from the same client without race conditions")
    void concurrentAccess() throws InterruptedException {
        int limit = 50;
        int threadCount = 20;
        int requestsPerThread = 10;
        service = buildService(config(List.of(), List.of(rule("1m", limit)), Map.of()));

        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();
        AtomicInteger denied = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        if (service.checkRateLimit("concurrent-user").isAllowed()) {
                            allowed.incrementAndGet();
                        } else {
                            denied.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        executor.shutdown();
        java.util.concurrent.TimeUnit.SECONDS.sleep(5);

        int total = allowed.get() + denied.get();
        assertThat(total).isEqualTo(threadCount * requestsPerThread);
        assertThat(allowed.get()).isEqualTo(limit);
        assertThat(denied.get()).isEqualTo(total - limit);
    }
}
