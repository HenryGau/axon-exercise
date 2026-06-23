package com.example.ratelimit.dto;

public class RateLimitError {
    private String message;

    public RateLimitError() {}

    public RateLimitError(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
