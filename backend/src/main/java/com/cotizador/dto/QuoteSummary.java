package com.cotizador.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.cotizador.entity.QuoteStatus;
import com.cotizador.entity.VehicleType;

/**
 * A quote in the list of {@code GET /api/quotes} (api.md §8.4). {@code clientName} is "First Last" for a DNI and the
 * business name for a CUIT.
 */
public record QuoteSummary(
		Long id,
		String quoteNumber,
		OffsetDateTime createdAt,
		String clientName,
		VehicleType vehicleType,
		String brandName,
		String modelName,
		String planName,
		BigDecimal totalPrice,
		String currency,
		QuoteStatus status) {
}
