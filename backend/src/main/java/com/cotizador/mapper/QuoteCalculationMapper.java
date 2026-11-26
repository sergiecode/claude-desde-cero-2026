package com.cotizador.mapper;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.cotizador.dto.QuoteCalculation;
import com.cotizador.dto.ZoneResponse;
import com.cotizador.service.PriceCalculation;

@Component
public class QuoteCalculationMapper {

	/** All amounts are in Argentine pesos. */
	public static final String CURRENCY = "ARS";

	private static final int MIN_RATE_SCALE = 2;

	public QuoteCalculation toResponse(PriceCalculation calculation) {
		return new QuoteCalculation(
				calculation.insuredAmount(),
				rate(calculation.planRate()),
				calculation.basePremium(),
				new ZoneResponse(calculation.zoneCode(), calculation.zoneName(), rate(calculation.zoneFactor())),
				rate(calculation.usageFactor()),
				calculation.vehicleAgeYears(),
				rate(calculation.vehicleAgeFactor()),
				calculation.driverAge(),
				rate(calculation.driverAgeFactor()),
				calculation.premium(),
				rate(calculation.taxRate()),
				calculation.taxes(),
				calculation.totalPrice(),
				CURRENCY);
	}

	/**
	 * Rates and factors are stored as NUMERIC(6,4); they are written without trailing zeros but with at least
	 * 2 decimals, as in api.md §8.2 ({@code 1.2000} → {@code 1.20}, {@code 0.0025} stays {@code 0.0025}).
	 * The value does not change.
	 */
	static BigDecimal rate(BigDecimal value) {
		BigDecimal stripped = value.stripTrailingZeros();
		return stripped.scale() < MIN_RATE_SCALE ? stripped.setScale(MIN_RATE_SCALE) : stripped;
	}

}
