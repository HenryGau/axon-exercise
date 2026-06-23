package com.example.ratelimit.service;

import com.example.ratelimit.config.ConfigLoader;
import com.example.ratelimit.config.RateLimitConfig;
import com.example.ratelimit.config.RateLimitRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService service;
    private SlidingWindowLog slidingWindowLog;

    @BeforeEach
    void setUp() {
        slidingWindowLog = new SlidingWindowLog();
        ConfigLoader configLoader = new ConfigLoader() {
            @Override
            public RateLimitConfig getConfig() {
                return new RateLimitConfig(
                    List.of("admin-1"),
                    List.of(new RateLimitRule("1m", 2)),
                    Map.of("vip", List.of(new RateLimitRule("1m", 5)))
                );
            }
        };
        service = new RateLimiterService();
        service.configLoader = configLoader;
        service.slidingWindowLog = slidingWindowLog;
    }

    @Test
    void shouldAllowAdmin() {
        assertThat(service.isAdmin("admin-1")).isTrue();
        assertThat(service.isAdmin("unknown")).isFalse();
    }

    @Test
    void shouldAllowWithinLimit() {
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
    }

    @Test
    void shouldDenyWhenExceedingLimit() {
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
        RateLimiterService.CheckResult result = service.checkRateLimit("user-1");
        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getReason()).isEqualTo("exceed 2 requests for last 1m");
    }

    @Test
    void shouldUseClientSpecificRules() {
        assertThat(service.checkRateLimit("vip").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("vip").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("vip").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("vip").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("vip").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("vip").isAllowed()).isFalse();
    }

    @Test
    void shouldReallowAfterWindowElapses() throws InterruptedException {
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("user-1").isAllowed()).isTrue();
        assertThat(service.checkRateLimit("user-1").isAllowed()).isFalse();

        // Use a 1ms window and sleep briefly
        RateLimiterService custom = new RateLimiterService();
        ConfigLoader loader = new ConfigLoader() {
            @Override
            public RateLimitConfig getConfig() {
                return new RateLimitConfig(
                    List.of(),
                    List.of(new RateLimitRule("1s", 2)),
                    Map.of()
                );
            }
        };
        custom.configLoader = loader;
        custom.slidingWindowLog = new SlidingWindowLog();

        assertThat(custom.checkRateLimit("user-2").isAllowed()).isTrue();
        assertThat(custom.checkRateLimit("user-2").isAllowed()).isTrue();
        assertThat(custom.checkRateLimit("user-2").isAllowed()).isFalse();

        Thread.sleep(1100);

        assertThat(custom.checkRateLimit("user-2").isAllowed()).isTrue();
    }
}
