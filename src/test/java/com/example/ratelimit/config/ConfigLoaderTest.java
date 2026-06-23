package com.example.ratelimit.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConfigLoader")
class ConfigLoaderTest {

    private Path tempConfig;
    private ConfigLoader loader;

    @AfterEach
    void tearDown() throws IOException {
        if (tempConfig != null) {
            Files.deleteIfExists(tempConfig);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        tempConfig = Files.createTempFile("rate-limits-", ".json");
        Files.writeString(tempConfig, """
            {
              "adminClientIds": ["admin-a"],
              "default": [
                { "window": "1m", "maxRequests": 10 }
              ],
              "clients": {}
            }
            """);
        loader = new ConfigLoader(tempConfig.toString());
    }

    @Test
    @DisplayName("loads config from file on first access")
    void loadsInitialConfig() {
        RateLimitConfig config = loader.getConfig();
        assertThat(config.getAdminClientIds()).containsExactly("admin-a");
        assertThat(config.getDefaultRules()).hasSize(1);
        assertThat(config.getDefaultRules().get(0).getMaxRequests()).isEqualTo(10);
    }

    @Test
    @DisplayName("reloads config when file is modified")
    void reloadsOnFileChange() throws IOException, InterruptedException {
        assertThat(loader.getConfig().getDefaultRules().get(0).getMaxRequests()).isEqualTo(10);

        // file modification time has second granularity on some OS
        Thread.sleep(1500);
        Files.writeString(tempConfig, """
            {
              "adminClientIds": ["admin-b"],
              "default": [
                { "window": "1m", "maxRequests": 25 }
              ],
              "clients": {}
            }
            """);

        RateLimitConfig reloaded = loader.getConfig();
        assertThat(reloaded.getAdminClientIds()).containsExactly("admin-b");
        assertThat(reloaded.getDefaultRules().get(0).getMaxRequests()).isEqualTo(25);
    }

    @Test
    @DisplayName("falls back to cached config when file is deleted after initial load")
    void returnsCachedOnReadFailure() throws IOException {
        loader.getConfig();
        Files.delete(tempConfig);

        RateLimitConfig config = loader.getConfig();
        assertThat(config.getDefaultRules().get(0).getMaxRequests()).isEqualTo(10);
    }
}
