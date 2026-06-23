package com.example.ratelimit.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RateLimitConfig {
    @JsonProperty("adminClientIds")
    private List<String> adminClientIds = Collections.emptyList();

    @JsonProperty("default")
    private List<RateLimitRule> defaultRules = Collections.emptyList();

    @JsonProperty("clients")
    private Map<String, List<RateLimitRule>> clients = Collections.emptyMap();

    public RateLimitConfig() {}

    public RateLimitConfig(List<String> adminClientIds, List<RateLimitRule> defaultRules, Map<String, List<RateLimitRule>> clients) {
        this.adminClientIds = adminClientIds;
        this.defaultRules = defaultRules;
        this.clients = clients;
    }

    public List<String> getAdminClientIds() {
        return adminClientIds;
    }

    public List<RateLimitRule> getDefaultRules() {
        return defaultRules;
    }

    public Map<String, List<RateLimitRule>> getClients() {
        return clients;
    }

    public List<RateLimitRule> getRulesForClient(String clientId) {
        List<RateLimitRule> clientRules = clients.get(clientId);
        if (clientRules != null && !clientRules.isEmpty()) {
            return clientRules;
        }
        return defaultRules;
    }

    public boolean isAdmin(String clientId) {
        return adminClientIds.contains(clientId);
    }
}
