package com.cotizador.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request of {@code POST /api/quotes/{id}/issue} (api.md §8.6). */
public record IssueRequest(
		@Schema(example = "2026-11-20") @NotNull(message = "Completá este campo") LocalDate startDate) {
}
