package com.brokeros.risk.api;

import java.util.List;

public record ErrorResponse(String path, List<Violation> violations) {

    public ErrorResponse {
        path = path == null ? "" : path;
        violations = violations == null ? List.of() : List.copyOf(violations);
    }

    public static ErrorResponse at(String path) {
        return new ErrorResponse(path, List.of());
    }

    public record Violation(String field, String message) {
    }
}
