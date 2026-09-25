package com.moviebooking.web.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.moviebooking.exception.ResourceNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Verifies the global handler maps exceptions to the shared {@link ApiError} shape.
 * Uses a throwaway controller so no real endpoint is introduced in this milestone.
 */
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsResourceNotFoundTo404() throws Exception {
        mockMvc.perform(get("/sample/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Movie not found: 123"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void mapsBodyValidationTo400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/sample")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void mapsUnexpectedExceptionTo500() throws Exception {
        mockMvc.perform(get("/sample/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @RestController
    static class SampleController {

        @GetMapping("/sample/missing")
        void missing() {
            throw ResourceNotFoundException.of("Movie", "123");
        }

        @GetMapping("/sample/boom")
        void boom() {
            throw new IllegalStateException("kaboom");
        }

        @PostMapping("/sample")
        void create(@Valid @RequestBody SampleRequest request) {
            // no-op; validation runs before we get here
        }
    }

    record SampleRequest(@NotBlank String name) {
    }
}
