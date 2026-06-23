package com.example.ratelimit.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

@QuarkusTest
@DisplayName("RateLimitFilter")
class RateLimitFilterTest {

    private void request(String clientId, int expectedStatus) {
        var req = given();
        if (clientId != null) {
            req = req.header("X-Client-ID", clientId);
        }
        req.when().get("/ping").then().statusCode(expectedStatus);
    }

    private void exhaustLimit(String clientId) {
        for (int i = 0; i < 3; i++) {
            request(clientId, 200);
        }
    }

    @Test
    @DisplayName("allows requests within the limit via X-Client-ID header")
    void withinLimitWithHeader() {
        request("normal-user", 200);
        request("normal-user", 200);
        request("normal-user", 200);
    }

    @Test
    @DisplayName("blocks with 429 when limit exceeded via header")
    void exceedsLimitWithHeader() {
        String client = "blocked-user";
        exhaustLimit(client);
        given()
            .header("X-Client-ID", client)
            .when().get("/ping")
            .then()
            .statusCode(429)
            .body("message", equalTo("exceed 3 requests for last 1m"));
    }

    @Test
    @DisplayName("bypasses rate limiting for admin clients")
    void adminBypass() {
        for (int i = 0; i < 10; i++) {
            request("test-admin", 200);
        }
    }

    @Test
    @DisplayName("resolves client ID from query parameter")
    void resolvesClientIdFromQueryParam() {
        given()
            .queryParam("clientId", "query-user")
            .when().get("/ping")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("blocks when limit exceeded via query param")
    void exceedsLimitWithQueryParam() {
        String client = "query-heavy";
        for (int i = 0; i < 3; i++) {
            given()
                .queryParam("clientId", client)
                .when().get("/ping")
                .then()
                .statusCode(200);
        }
        given()
            .queryParam("clientId", client)
            .when().get("/ping")
            .then()
            .statusCode(429)
            .body("message", startsWith("exceed"));
    }

    @Test
    @DisplayName("resolves client ID from X-Forwarded-For header")
    void forwardsFromXForwardedFor() {
        String ip = "10.0.0.1";
        given()
            .header("X-Forwarded-For", ip)
            .when().get("/ping")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName("rate limits by X-Forwarded-For IP when no client ID is provided")
    void rateLimitsByXForwardedFor() {
        String ip = "10.0.0.2";
        for (int i = 0; i < 3; i++) {
            given()
                .header("X-Forwarded-For", ip)
                .when().get("/ping")
                .then()
                .statusCode(200);
        }

        given()
            .header("X-Forwarded-For", ip)
            .when().get("/ping")
            .then()
            .statusCode(429);
    }

    @Test
    @DisplayName("passes through requests without client identification")
    void noClientId() {
        given()
            .when().get("/ping")
            .then()
            .statusCode(200)
            .body(equalTo("pong"));
    }

    @Test
    @DisplayName("different clients do not interfere")
    void clientIsolation() {
        String heavy = "isolation-heavy";
        String light = "isolation-light";

        exhaustLimit(heavy);

        given()
            .header("X-Client-ID", heavy)
            .when().get("/ping")
            .then()
            .statusCode(429);

        request(light, 200);
        request(light, 200);
    }

    @Test
    @DisplayName("returns 429 with correct message format")
    void correctErrorMessage() {
        String client = "msg-test";
        exhaustLimit(client);

        given()
            .header("X-Client-ID", client)
            .when().get("/ping")
            .then()
            .statusCode(429)
            .body("message", equalTo("exceed 3 requests for last 1m"));
    }


}
