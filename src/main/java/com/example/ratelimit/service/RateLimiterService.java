package com.example.ratelimit.service;

import com.example.ratelimit.config.ConfigLoader;
import com.example.ratelimit.config.RateLimitConfig;
import com.example.ratelimit.config.RateLimitRule;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class RateLimiterService {

    @Inject
    ConfigLoader configLoader;

    @Inject
    SlidingWindowLog slidingWindowLog;

    public boolean isAdmin(String clientId) {
        return configLoader.getConfig().isAdmin(clientId);
    }

    public CheckResult checkRateLimit(String clientId) {
        RateLimitConfig config = configLoader.getConfig();
        List<RateLimitRule> rules = config.getRulesForClient(clientId);

        for (RateLimitRule rule : rules) {
            boolean allowed = slidingWindowLog.tryAcquire(clientId, rule.parseDuration(), rule.getMaxRequests());
            if (!allowed) {
                return CheckResult.denied(rule.getMaxRequests(), rule.windowDescription());
            }
        }
        return CheckResult.allowed();
    }

    public static class CheckResult {
        private final boolean allowed;
        private final String reason;

        private CheckResult(boolean allowed, String reason) {
            this.allowed = allowed;
            this.reason = reason;
        }

        public static CheckResult allowed() {
            return new CheckResult(true, null);
        }

        public static CheckResult denied(int maxRequests, String window) {
            return new CheckResult(false, "exceed " + maxRequests + " requests for last " + window);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public String getReason() {
            return reason;
        }
    }
}
