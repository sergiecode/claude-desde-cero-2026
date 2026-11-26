package com.cotizador.service;

import java.math.BigDecimal;

/**
 * Result of the price calculation (backend.md §4.3, manual §7) with every value used in it, so a quote can keep a
 * copy of them (rule Q2). Money has scale 2; {@code driverAge} is null for companies (CUIT).
 */
public record PriceCalculation(
		BigDecimal insuredAmount,
		BigDecimal planRate,
		BigDecimal basePremium,
		String zoneCode,
		String zoneName,
		BigDecimal zoneFactor,
		BigDecimal usageFactor,
		int vehicleAgeYears,
		BigDecimal vehicleAgeFactor,
		Integer driverAge,
		BigDecimal driverAgeFactor,
		BigDecimal premium,
		BigDecimal taxRate,
		BigDecimal taxes,
		BigDecimal totalPrice) {
}
