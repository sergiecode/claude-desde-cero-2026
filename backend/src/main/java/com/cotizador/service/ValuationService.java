package com.cotizador.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.repository.VehicleVersionRepository;

/** Insured amount of a vehicle (backend.md §4.2, rules S1 to S4 and S6). */
@Service
public class ValuationService {

	private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);
	private static final int MONEY_SCALE = 2;

	private final VehicleVersionRepository versionRepository;
	private final PricingProperties pricingProperties;
	private final Clock clock;

	public ValuationService(VehicleVersionRepository versionRepository, PricingProperties pricingProperties,
			Clock clock) {
		this.versionRepository = versionRepository;
		this.pricingProperties = pricingProperties;
		this.clock = clock;
	}

	/**
	 * Insured amount range for the version and manufacturing year. The year is validated first
	 * ({@code INVALID_YEAR}, rule V3), then the valuation is looked up ({@code VALUATION_NOT_FOUND}, rule S6).
	 */
	@Transactional(readOnly = true)
	public InsuredValueRange insuredValues(long versionId, int manufacturingYear) {
		validateYear(manufacturingYear);
		return rangeFor(versionId, manufacturingYear);
	}

	/**
	 * Rule V3: the year must be between {@code app.pricing.min-vehicle-year} and the current year of the clock.
	 *
	 * @throws BusinessException {@code INVALID_YEAR}
	 */
	public void validateYear(int manufacturingYear) {
		int currentYear = currentYear();
		if (manufacturingYear < pricingProperties.minVehicleYear() || manufacturingYear > currentYear) {
			throw new BusinessException(ErrorCode.INVALID_YEAR, pricingProperties.minVehicleYear(), currentYear);
		}
	}

	/**
	 * Range for the version's reference value, without validating the year.
	 *
	 * @throws BusinessException {@code VALUATION_NOT_FOUND} if the version does not exist or has no reference value
	 */
	@Transactional(readOnly = true)
	public InsuredValueRange rangeFor(long versionId, int manufacturingYear) {
		BigDecimal referenceValue = versionRepository.findById(versionId)
				.map(version -> version.getReferenceValue())
				.orElseThrow(() -> new BusinessException(ErrorCode.VALUATION_NOT_FOUND));
		if (referenceValue == null) {
			throw new BusinessException(ErrorCode.VALUATION_NOT_FOUND);
		}
		return calculate(referenceValue, manufacturingYear);
	}

	/** Pure calculation of backend.md §4.2 for a 0 km reference value and a manufacturing year. */
	public InsuredValueRange calculate(BigDecimal referenceValue, int manufacturingYear) {
		int vehicleAge = currentYear() - manufacturingYear;
		BigDecimal depreciated = BigDecimal.ONE
				.subtract(pricingProperties.depreciationRatePerYear().multiply(BigDecimal.valueOf(vehicleAge)));
		BigDecimal depreciationFactor = depreciated.max(pricingProperties.depreciationFloor());

		BigDecimal suggested = roundToThousands(referenceValue.multiply(depreciationFactor));
		BigDecimal minimum = roundToThousands(suggested.multiply(pricingProperties.insuredAmountMinRatio()));
		BigDecimal maximum = roundToThousands(suggested.multiply(pricingProperties.insuredAmountMaxRatio()));
		return new InsuredValueRange(minimum, suggested, maximum);
	}

	/** Rule S4: divided by 1000, rounded to an integer with HALF_UP, multiplied by 1000, scale 2. */
	static BigDecimal roundToThousands(BigDecimal amount) {
		return amount.divide(THOUSAND, 0, RoundingMode.HALF_UP)
				.multiply(THOUSAND)
				.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

	private int currentYear() {
		return LocalDate.now(clock).getYear();
	}

}
