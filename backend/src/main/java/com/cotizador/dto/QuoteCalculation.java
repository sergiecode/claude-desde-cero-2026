package com.cotizador.dto;

import java.math.BigDecimal;

/**
 * Price breakdown of a quote (api.md §8.2). Money has 2 decimals; {@code driverAge} is null for a company.
 */
public record QuoteCalculation(
		BigDecimal insuredAmount,
		BigDecimal planRate,
		BigDecimal basePremium,
		ZoneResponse zone,
		BigDecimal usageFactor,
		int vehicleAgeYears,
		BigDecimal vehicleAgeFactor,
		Integer driverAge,
		BigDecimal driverAgeFactor,
		BigDecimal premium,
		BigDecimal taxRate,
		BigDecimal taxes,
		BigDecimal totalPrice,
		String currency) {
}
