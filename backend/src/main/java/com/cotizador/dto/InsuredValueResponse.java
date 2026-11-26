package com.cotizador.dto;

import java.math.BigDecimal;

/** Insured amount range of a vehicle (api.md §7). */
public record InsuredValueResponse(BigDecimal minimumAmount, BigDecimal suggestedAmount, BigDecimal maximumAmount,
		String currency) {
}
