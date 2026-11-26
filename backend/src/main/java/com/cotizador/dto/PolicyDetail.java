package com.cotizador.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.cotizador.entity.PolicyStatus;

/**
 * An issued policy (api.md §9.1). Client, vehicle and plan are read from its quote, which is never modified; the
 * amounts are the copies stored in the policy (rule E6).
 */
public record PolicyDetail(
		Long id,
		String policyNumber,
		PolicyStatus status,
		OffsetDateTime issuedAt,
		LocalDate startDate,
		LocalDate endDate,
		Long quoteId,
		String quoteNumber,
		ClientResponse client,
		QuoteVehicle vehicle,
		String planName,
		BigDecimal insuredAmount,
		BigDecimal premium,
		BigDecimal taxes,
		BigDecimal totalPrice,
		String currency) {
}
