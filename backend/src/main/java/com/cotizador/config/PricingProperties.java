package com.cotizador.config;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Business parameters under {@code app.pricing.*} (backend.md §3).
 */
@Validated
@ConfigurationProperties(prefix = "app.pricing")
public record PricingProperties(
		@NotNull BigDecimal taxRate,
		@NotNull BigDecimal companyDriverAgeFactor,
		@NotNull Integer quoteValidityDays,
		@NotNull Integer minVehicleYear,
		@NotNull Integer minClientAge,
		@NotNull BigDecimal depreciationRatePerYear,
		@NotNull BigDecimal depreciationFloor,
		@NotNull BigDecimal insuredAmountMinRatio,
		@NotNull BigDecimal insuredAmountMaxRatio,
		@NotNull Integer maxStartDateDaysAhead) {
}
