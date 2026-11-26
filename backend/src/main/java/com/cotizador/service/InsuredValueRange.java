package com.cotizador.service;

import java.math.BigDecimal;

/**
 * Insured amount range of a vehicle (rules S2, S3 and S4): suggested amount and the minimum and maximum the
 * producer may choose. All amounts are rounded to thousands, with scale 2.
 */
public record InsuredValueRange(BigDecimal minimumAmount, BigDecimal suggestedAmount, BigDecimal maximumAmount) {

	/** Rule S5: whether the amount is within [minimum, maximum], both inclusive. */
	public boolean contains(BigDecimal amount) {
		return amount.compareTo(minimumAmount) >= 0 && amount.compareTo(maximumAmount) <= 0;
	}

}
