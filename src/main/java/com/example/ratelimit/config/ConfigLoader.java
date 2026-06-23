package com.example.ratelimit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@ApplicationScoped
public class ConfigLoader {

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile RateLimitConfig cachedConfig;
    private volatile long lastModified = 0;
    private volatile String configPath;

    public ConfigLoader() {
        this.configPath = System.getProperty("rate.limit.config.path", "rate-limits.json");
    }

    public RateLimitConfig getConfig() {
        Path externalPath = Paths.get(configPath);
        if (Files.exists(externalPath)) {
            try {
                long currentModified = Files.getLastModifiedTime(externalPath).toMillis();
                if (currentModified > lastModified || cachedConfig == null) {
                    try (InputStream is = new FileInputStream(externalPath.toFile())) {
                        cachedConfig = mapper.readValue(is, RateLimitConfig.class);
                        lastModified = currentModified;
                    }
                }
            } catch (Exception e) {
                if (cachedConfig == null) {
                    cachedConfig = loadFallback();
                }
            }
        } else {
            if (cachedConfig == null) {
                cachedConfig = loadFallback();
            }
        }
        return cachedConfig;
    }

    private RateLimitConfig loadFallback() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("rate-limits.json")) {
            if (is != null) {
                return mapper.readValue(is, RateLimitConfig.class);
            }
        } catch (Exception ignored) {}
        return new RateLimitConfig();
    }
}
