package com.cotizador.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT settings under {@code app.jwt.*} (backend.md §3 and §9).
 * The secret comes from {@code JWT_SECRET} and must have at least 32 characters (README.md §4).
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32) String secret,
		@NotNull Duration expiration) {
}
