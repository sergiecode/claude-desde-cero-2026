package com.cotizador.exception;

/** One entry of the {@code errors} list in a Problem Details response (api.md §2). */
public record FieldErrorResponse(String field, String message) {
}
