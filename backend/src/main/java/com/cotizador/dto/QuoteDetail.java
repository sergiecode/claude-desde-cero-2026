package com.cotizador.dto;

import java.time.OffsetDateTime;

import com.cotizador.entity.QuoteStatus;

/**
 * A saved quote (api.md §8.5). {@code client.age} is the driver age stored when it was quoted, not recalculated.
 * {@code policy} is null while the quote has not been issued.
 */
public record QuoteDetail(
		Long id,
		String quoteNumber,
		QuoteStatus status,
		OffsetDateTime createdAt,
		OffsetDateTime validUntil,
		ClientResponse client,
		QuoteVehicle vehicle,
		String planName,
		QuoteCalculation calculation,
		QuotePolicy policy) {
}
