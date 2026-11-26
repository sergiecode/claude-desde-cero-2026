package com.cotizador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyInt;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

/** Price calculation (backend.md §4.3, manual §7), with the factors of the seed data. */
class PricingServiceTest {

	private static final PricingProperties PROPERTIES = new PricingProperties(
			new BigDecimal("0.21"), new BigDecimal("1.00"), 15, 2010, 18,
			new BigDecimal("0.05"), new BigDecimal("0.30"), new BigDecimal("0.80"), new BigDecimal("1.20"), 30);

	private final UsageFactorRepository usageFactorRepository = mock(UsageFactorRepository.class);
	private final VehicleAgeFactorRepository vehicleAgeFactorRepository = mock(VehicleAgeFactorRepository.class);
	private final DriverAgeFactorRepository driverAgeFactorRepository = mock(DriverAgeFactorRepository.class);
	private final PricingService service = new PricingService(usageFactorRepository, vehicleAgeFactorRepository,
			driverAgeFactorRepository, PROPERTIES);

	@BeforeEach
	void seedFactors() {
		usage(VehicleUsageType.PARTICULAR, "1.0000");
		usage(VehicleUsageType.AGRICOLA, "1.0500");
		usage(VehicleUsageType.TRANSPORTE_CARGA, "1.2500");
		usage(VehicleUsageType.DELIVERY, "1.3000");
		vehicleAge(2, "1.0000");
		vehicleAge(8, "1.1000");
		vehicleAge(12, "1.2000");
		vehicleAge(16, "1.2000");
		driverAge(22, "1.3000");
		driverAge(27, "1.1500");
		driverAge(72, "1.1500");
	}

	@Test
	void AC_PRICE_01_autoInCabaYoungDriver() {
		PriceCalculation price = service.calculate(new BigDecimal("10800000.00"), new BigDecimal("0.0025"),
				zone("ZONA_1", "CABA", "1.2000"), VehicleUsageType.PARTICULAR, 8, 22);

		assertThat(price.basePremium()).isEqualTo(new BigDecimal("27000.00"));
		assertThat(price.zoneCode()).isEqualTo("ZONA_1");
		assertThat(price.usageFactor()).isEqualByComparingTo("1.00");
		assertThat(price.vehicleAgeYears()).isEqualTo(8);
		assertThat(price.vehicleAgeFactor()).isEqualByComparingTo("1.10");
		assertThat(price.driverAge()).isEqualTo(22);
		assertThat(price.driverAgeFactor()).isEqualByComparingTo("1.30");
		assertAmounts(price, "46332.00", "9729.72", "56061.72");
	}

	@Test
	void AC_PRICE_02_companyTruckInRosarioUsesCompanyDriverFactor() {
		PriceCalculation price = service.calculate(new BigDecimal("50000000.00"), new BigDecimal("0.0010"),
				zone("ZONA_5", "Rosario", "1.1000"), VehicleUsageType.TRANSPORTE_CARGA, 12, null);

		assertThat(price.basePremium()).isEqualTo(new BigDecimal("50000.00"));
		assertThat(price.driverAge()).isNull();
		assertThat(price.driverAgeFactor()).isEqualByComparingTo("1.00");
		assertThat(price.vehicleAgeFactor()).isEqualByComparingTo("1.20");
		assertAmounts(price, "82500.00", "17325.00", "99825.00");
		verify(driverAgeFactorRepository, never()).findForAge(anyInt());
	}

	@Test
	void AC_PRICE_03_motoDeliveryRoundsOnlyAtTheEnd() {
		PriceCalculation price = service.calculate(new BigDecimal("2250000.00"), new BigDecimal("0.0045"),
				zone("ZONA_2", "Gran Buenos Aires", "1.1500"), VehicleUsageType.DELIVERY, 2, 27);

		assertThat(price.vehicleAgeFactor()).isEqualByComparingTo("1.00");
		assertThat(price.driverAgeFactor()).isEqualByComparingTo("1.15");
		assertAmounts(price, "17407.41", "3655.56", "21062.97");
	}

	@Test
	void AC_PRICE_04_maximumAgeAndVehicleAgeFactors() {
		PriceCalculation price = service.calculate(new BigDecimal("15600000.00"), new BigDecimal("0.0025"),
				zone("ZONA_7", "Interior del país", "0.9500"), VehicleUsageType.AGRICOLA, 16, 72);

		assertThat(price.vehicleAgeFactor()).isEqualByComparingTo("1.20");
		assertThat(price.driverAgeFactor()).isEqualByComparingTo("1.15");
		assertAmounts(price, "53685.45", "11273.94", "64959.39");
	}

	@Test
	void premiumIsRoundedOnceAndDoesNotStartFromTheRoundedBasePremium() {
		// 1,000,002 × 0.0025 = 2,500.005 → basePremium 2,500.01.
		// Full precision: 2,500.005 × 0.95 × 1 × 1 × 1 = 2,375.00475 → 2,375.00.
		// Starting from the rounded base it would be 2,500.01 × 0.95 = 2,375.0095 → 2,375.01 (wrong).
		PriceCalculation price = service.calculate(new BigDecimal("1000002.00"), new BigDecimal("0.0025"),
				zone("ZONA_7", "Interior del país", "0.9500"), VehicleUsageType.PARTICULAR, 2, null);

		assertThat(price.basePremium()).isEqualTo(new BigDecimal("2500.01"));
		assertThat(price.premium()).isEqualTo(new BigDecimal("2375.00"));
	}

	@Test
	void taxesRoundHalfUpOnTheRoundedPremium() {
		// premium 2,000,010 × 0.0050 = 10,000.05; taxes 10,000.05 × 0.21 = 2,100.0105 → 2,100.01
		PriceCalculation price = service.calculate(new BigDecimal("2000010.00"), new BigDecimal("0.0050"),
				zone("ZONA_6", "Interior de Buenos Aires", "1.0000"), VehicleUsageType.PARTICULAR, 2, null);

		assertAmounts(price, "10000.05", "2100.01", "12100.06");
	}

	@Test
	void missingVehicleAgeFactorIsAnInternalError() {
		when(vehicleAgeFactorRepository.findForAge(99)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.calculate(new BigDecimal("1000000.00"), new BigDecimal("0.0025"),
				zone("ZONA_1", "CABA", "1.2000"), VehicleUsageType.PARTICULAR, 99, 22))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getCode())
				.isEqualTo(ErrorCode.INTERNAL_ERROR);
	}

	@Test
	void missingUsageFactorIsAnInternalError() {
		when(usageFactorRepository.findById(VehicleUsageType.TAXI_REMIS)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.calculate(new BigDecimal("1000000.00"), new BigDecimal("0.0025"),
				zone("ZONA_1", "CABA", "1.2000"), VehicleUsageType.TAXI_REMIS, 2, 22))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getCode())
				.isEqualTo(ErrorCode.INTERNAL_ERROR);
	}

	@Test
	void missingDriverAgeFactorIsAnInternalError() {
		when(driverAgeFactorRepository.findForAge(17)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.calculate(new BigDecimal("1000000.00"), new BigDecimal("0.0025"),
				zone("ZONA_1", "CABA", "1.2000"), VehicleUsageType.PARTICULAR, 2, 17))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getCode())
				.isEqualTo(ErrorCode.INTERNAL_ERROR);
	}

	private static void assertAmounts(PriceCalculation price, String premium, String taxes, String totalPrice) {
		assertThat(price.premium()).isEqualTo(new BigDecimal(premium));
		assertThat(price.taxes()).isEqualTo(new BigDecimal(taxes));
		assertThat(price.totalPrice()).isEqualTo(new BigDecimal(totalPrice));
	}

	private void usage(VehicleUsageType type, String factor) {
		UsageFactor usageFactor = new UsageFactor();
		usageFactor.setUsageType(type);
		usageFactor.setFactor(new BigDecimal(factor));
		when(usageFactorRepository.findById(type)).thenReturn(Optional.of(usageFactor));
	}

	private void vehicleAge(int years, String factor) {
		VehicleAgeFactor ageFactor = new VehicleAgeFactor();
		ageFactor.setFactor(new BigDecimal(factor));
		when(vehicleAgeFactorRepository.findForAge(years)).thenReturn(Optional.of(ageFactor));
	}

	private void driverAge(int age, String factor) {
		DriverAgeFactor ageFactor = new DriverAgeFactor();
		ageFactor.setFactor(new BigDecimal(factor));
		when(driverAgeFactorRepository.findForAge(age)).thenReturn(Optional.of(ageFactor));
	}

	private static TariffZone zone(String code, String name, String factor) {
		TariffZone zone = new TariffZone();
		zone.setCode(code);
		zone.setName(name);
		zone.setFactor(new BigDecimal(factor));
		return zone;
	}

}
