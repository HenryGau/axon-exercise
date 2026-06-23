package com.example.ratelimit.filter;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;

@QuarkusTest
class RateLimitFilterTest {

    @Test
    void shouldAllowRequestWithinLimit() {
        given()
            .header("X-Client-ID", "user-1")
            .when().get("/ping")
            .then()
            .statusCode(200)
            .body(equalTo("pong"));
    }

    @Test
    void shouldBlockWhenExceedingLimit() {
        String clientId = "heavy-user";

        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);
        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);
        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);

        given()
            .header("X-Client-ID", clientId)
            .when().get("/ping")
            .then()
            .statusCode(429)
            .body("message", startsWith("exceed"));
    }

    @Test
    void shouldNotRateLimitAdminClient() {
        given()
            .header("X-Client-ID", "test-admin")
            .when().get("/ping")
            .then()
            .statusCode(200);

        given()
            .header("X-Client-ID", "test-admin")
            .when().get("/ping")
            .then()
            .statusCode(200);

        given()
            .header("X-Client-ID", "test-admin")
            .when().get("/ping")
            .then()
            .statusCode(200);

        given()
            .header("X-Client-ID", "test-admin")
            .when().get("/ping")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldResolveClientIdFromQueryParam() {
        given()
            .queryParam("clientId", "query-user")
            .when().get("/ping")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldReturn429WithCorrectMessage() {
        String clientId = "blocked-user";

        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);
        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);
        given().header("X-Client-ID", clientId).when().get("/ping").then().statusCode(200);

        given()
            .header("X-Client-ID", clientId)
            .when().get("/ping")
            .then()
            .statusCode(429)
            .body("message", equalTo("exceed 3 requests for last 1m"));
    }
}
