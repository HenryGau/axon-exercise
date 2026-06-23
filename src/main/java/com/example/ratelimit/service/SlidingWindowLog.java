package com.example.ratelimit.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@ApplicationScoped
public class SlidingWindowLog {

    private final Map<String, Map<Duration, Deque<Instant>>> store = new ConcurrentHashMap<>();

    public boolean tryAcquire(String clientId, Duration window, int maxRequests) {
        Map<Duration, Deque<Instant>> clientWindows = store.computeIfAbsent(clientId, k -> new ConcurrentHashMap<>());
        Deque<Instant> timestamps = clientWindows.computeIfAbsent(window, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            Instant cutoff = Instant.now().minus(window);
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(cutoff)) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(Instant.now());
            return true;
        }
    }
}
