package com.moviebooking.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Minimal liveness endpoint used to confirm the app is up during early
 * scaffolding. Real domain endpoints arrive in later phases.
 */
@RestController
public class PingController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of(
                "service", "movie-ticket-booking",
                "status", "UP",
                "time", Instant.now().toString());
    }
}
