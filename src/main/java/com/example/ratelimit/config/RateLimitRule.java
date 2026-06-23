package com.example.ratelimit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;

public class RateLimitRule {
    private String window;
    @JsonProperty("maxRequests")
    private int maxRequests;

    public RateLimitRule() {}

    public RateLimitRule(String window, int maxRequests) {
        this.window = window;
        this.maxRequests = maxRequests;
    }

    public String getWindow() {
        return window;
    }

    public void setWindow(String window) {
        this.window = window;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration parseDuration() {
        if (window.endsWith("s")) {
            return Duration.ofSeconds(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("m")) {
            return Duration.ofMinutes(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("h")) {
            return Duration.ofHours(Long.parseLong(window.substring(0, window.length() - 1)));
        } else if (window.endsWith("d")) {
            return Duration.ofDays(Long.parseLong(window.substring(0, window.length() - 1)));
        }
        throw new IllegalArgumentException("Unsupported window format: " + window);
    }

    public String windowDescription() {
        return window;
    }
}
