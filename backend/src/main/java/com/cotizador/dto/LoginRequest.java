package com.cotizador.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank(message = "Completá este campo") String email,
		@NotBlank(message = "Completá este campo") String password) {
}
