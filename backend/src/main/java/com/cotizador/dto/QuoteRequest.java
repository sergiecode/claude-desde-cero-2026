package com.cotizador.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.cotizador.entity.VehicleType;
import com.cotizador.entity.VehicleUsageType;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request shared by {@code POST /api/quotes/calculate} and {@code POST /api/quotes} (api.md §8.1). Every field is
 * required. The patent is normalized (uppercase, no spaces) by the service before it is validated.
 */
public record QuoteRequest(
		@Schema(example = "1") @NotNull(message = REQUIRED) Long clientId,
		@Schema(example = "AUTO") @NotNull(message = REQUIRED) VehicleType vehicleType,
		@Schema(example = "2018") @NotNull(message = REQUIRED) Integer manufacturingYear,
		@Schema(example = "2") @NotNull(message = REQUIRED) Long brandId,
		@Schema(example = "3") @NotNull(message = REQUIRED) Long modelId,
		@Schema(example = "5") @NotNull(message = REQUIRED) Long versionId,
		@Schema(example = "PARTICULAR") @NotNull(message = REQUIRED) VehicleUsageType usageType,
		@Schema(example = "abc123") @NotBlank(message = REQUIRED) String patent,
		@Schema(example = "2") @NotNull(message = REQUIRED) Long planId,
		@Schema(example = "10800000.00")
		@NotNull(message = REQUIRED)
		@Positive(message = INVALID)
		@Digits(integer = 13, fraction = 2, message = INVALID) BigDecimal insuredAmount) {

	static final String REQUIRED = "Completá este campo";
	static final String INVALID = "Revisá los datos ingresados";

}
