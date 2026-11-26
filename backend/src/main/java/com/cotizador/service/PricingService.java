package com.cotizador.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.entity.DriverAgeFactor;
import com.cotizador.entity.TariffZone;
import com.cotizador.entity.UsageFactor;
import com.cotizador.entity.VehicleAgeFactor;
import com.cotizador.entity.VehicleUsageType;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.repository.DriverAgeFactorRepository;
import com.cotizador.repository.UsageFactorRepository;
import com.cotizador.repository.VehicleAgeFactorRepository;

/** Monthly price of a quote (backend.md §4.3, manual §7). */
@Service
public class PricingService {

	private static final Logger log = LoggerFactory.getLogger(PricingService.class);

	private static final int MONEY_SCALE = 2;

	private final UsageFactorRepository usageFactorRepository;
	private final VehicleAgeFactorRepository vehicleAgeFactorRepository;
	private final DriverAgeFactorRepository driverAgeFactorRepository;
	private final PricingProperties pricingProperties;

	public PricingService(UsageFactorRepository usageFactorRepository,
			VehicleAgeFactorRepository vehicleAgeFactorRepository,
			DriverAgeFactorRepository driverAgeFactorRepository, PricingProperties pricingProperties) {
		this.usageFactorRepository = usageFactorRepository;
		this.vehicleAgeFactorRepository = vehicleAgeFactorRepository;
		this.driverAgeFactorRepository = driverAgeFactorRepository;
		this.pricingProperties = pricingProperties;
	}

	/**
	 * Calculates the price. The premium multiplies every factor with full precision and is rounded once at the
	 * end; {@code basePremium} is only informative and is not the starting point of the premium.
	 *
	 * @param insuredAmount   insured amount chosen by the producer
	 * @param planRate        monthly rate of the plan, as a fraction
	 * @param zone            tariff zone of the client's city (rule C7)
	 * @param usageType       vehicle usage type
	 * @param vehicleAgeYears current year minus manufacturing year
	 * @param driverAge       client age for a DNI client; null for a company (CUIT), which uses
	 *                        {@code app.pricing.company-driver-age-factor}
	 * @throws BusinessException {@code INTERNAL_ERROR} if a factor is missing from the tariff tables
	 */
	@Transactional(readOnly = true)
	public PriceCalculation calculate(BigDecimal insuredAmount, BigDecimal planRate, TariffZone zone,
			VehicleUsageType usageType, int vehicleAgeYears, Integer driverAge) {
		BigDecimal usageFactor = usageFactorRepository.findById(usageType)
				.map(UsageFactor::getFactor)
				.orElseThrow(() -> missingFactor("usage factor for " + usageType));
		BigDecimal vehicleAgeFactor = vehicleAgeFactorRepository.findForAge(vehicleAgeYears)
				.map(VehicleAgeFactor::getFactor)
				.orElseThrow(() -> missingFactor("vehicle age factor for " + vehicleAgeYears + " years"));
		BigDecimal driverAgeFactor = driverAgeFactor(driverAge);
		BigDecimal taxRate = pricingProperties.taxRate();

		BigDecimal basePremium = round2(insuredAmount.multiply(planRate));
		BigDecimal premium = round2(insuredAmount
				.multiply(planRate)
				.multiply(zone.getFactor())
				.multiply(usageFactor)
				.multiply(vehicleAgeFactor)
				.multiply(driverAgeFactor));
		BigDecimal taxes = round2(premium.multiply(taxRate));
		BigDecimal totalPrice = premium.add(taxes);

		return new PriceCalculation(insuredAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP), planRate,
				basePremium, zone.getCode(), zone.getName(), zone.getFactor(), usageFactor, vehicleAgeYears,
				vehicleAgeFactor, driverAge, driverAgeFactor, premium, taxRate, taxes, totalPrice);
	}

	private BigDecimal driverAgeFactor(Integer driverAge) {
		if (driverAge == null) {
			return pricingProperties.companyDriverAgeFactor();
		}
		return driverAgeFactorRepository.findForAge(driverAge)
				.map(DriverAgeFactor::getFactor)
				.orElseThrow(() -> missingFactor("driver age factor for " + driverAge + " years"));
	}

	private static BusinessException missingFactor(String description) {
		log.error("Incomplete tariff configuration: no {}", description);
		return new BusinessException(ErrorCode.INTERNAL_ERROR);
	}

	/** {@code round2} of backend.md §4.3. */
	static BigDecimal round2(BigDecimal amount) {
		return amount.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
	}

}
