package com.moviebooking.web.error;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The single error response shape returned by every endpoint. {@code fieldErrors} is
 * only populated for validation failures and is omitted from the JSON otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ValidationError> fieldErrors) {

    public record ValidationError(String field, String message) {
    }
}
