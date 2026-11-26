package com.cotizador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cotizador.config.PricingProperties;
import com.cotizador.entity.VehicleVersion;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.repository.VehicleVersionRepository;

/** Insured amount calculation (backend.md §4.2, rules S2, S3, S4, S6 and V3), with the clock fixed on 2026-11-19. */
class ValuationServiceTest {

	private static final Clock CLOCK = Clock.fixed(OffsetDateTime.parse("2026-11-19T10:00:00-03:00").toInstant(),
			ZoneId.of("America/Argentina/Buenos_Aires"));

	private static final PricingProperties PROPERTIES = new PricingProperties(
			new BigDecimal("0.21"), new BigDecimal("1.00"), 15, 2010, 18,
			new BigDecimal("0.05"), new BigDecimal("0.30"), new BigDecimal("0.80"), new BigDecimal("1.20"), 30);

	private final VehicleVersionRepository versionRepository = mock(VehicleVersionRepository.class);
	private final ValuationService service = new ValuationService(versionRepository, PROPERTIES, CLOCK);

	@Test
	void AC_VAL_01_S2_S3_eightYearOldVehicleDepreciatesFortyPercent() {
		InsuredValueRange range = service.calculate(new BigDecimal("18000000.00"), 2018);

		assertRange(range, "8640000.00", "10800000.00", "12960000.00");
	}

	@Test
	void AC_VAL_02_S2_S3_twelveYearOldVehicleDepreciatesSixtyPercent() {
		InsuredValueRange range = service.calculate(new BigDecimal("110000000.00"), 2014);

		assertRange(range, "35200000.00", "44000000.00", "52800000.00");
	}

	@Test
	void AC_VAL_03_S2_sixteenYearOldVehicleStopsAtThirtyPercentFloor() {
		InsuredValueRange range = service.calculate(new BigDecimal("52000000.00"), 2010);

		assertRange(range, "12480000.00", "15600000.00", "18720000.00");
	}

	@ParameterizedTest
	@CsvSource({
			"2026, 10000000.00", // age 0: 100 %
			"2025,  9500000.00", // age 1: 95 %
			"2013,  3500000.00", // age 13: 35 %
			"2012,  3000000.00", // age 14: exactly the floor
			"2011,  3000000.00", // age 15: floor
			"2010,  3000000.00"  // age 16: floor
	})
	void S2_depreciationIsFivePercentPerYearWithThirtyPercentFloor(int year, String expectedSuggested) {
		InsuredValueRange range = service.calculate(new BigDecimal("10000000.00"), year);

		assertThat(range.suggestedAmount()).isEqualTo(new BigDecimal(expectedSuggested));
	}

	@Test
	void S3_S4_minimumAndMaximumAreRoundedFromTheRoundedSuggested() {
		// 12,345,678 × 0.60 = 7,407,406.80 → 7,407,000; × 0.80 = 5,925,600 → 5,926,000; × 1.20 = 8,888,400 → 8,888,000
		InsuredValueRange range = service.calculate(new BigDecimal("12345678.00"), 2018);

		assertRange(range, "5926000.00", "7407000.00", "8888000.00");
	}

	@ParameterizedTest
	@CsvSource({
			"10500.00, 11000.00",
			"10499.99, 10000.00",
			"999.99, 1000.00",
			"499.99, 0.00",
			"7407406.80, 7407000.00"
	})
	void S4_roundToThousandsUsesHalfUpWithScaleTwo(String amount, String expected) {
		BigDecimal rounded = ValuationService.roundToThousands(new BigDecimal(amount));

		assertThat(rounded).isEqualTo(new BigDecimal(expected));
		assertThat(rounded.scale()).isEqualTo(2);
	}

	@Test
	void S2_rangeForLooksUpTheReferenceValueOfTheVersion() {
		when(versionRepository.findById(5L)).thenReturn(Optional.of(version(new BigDecimal("18000000.00"))));

		assertRange(service.insuredValues(5L, 2018), "8640000.00", "10800000.00", "12960000.00");
	}

	@Test
	void AC_VAL_06_S6_versionWithoutReferenceValueHasNoValuation() {
		when(versionRepository.findById(99L)).thenReturn(Optional.of(version(null)));

		assertBusinessError(() -> service.insuredValues(99L, 2018), ErrorCode.VALUATION_NOT_FOUND);
	}

	@Test
	void S6_unknownVersionHasNoValuation() {
		when(versionRepository.findById(999L)).thenReturn(Optional.empty());

		assertBusinessError(() -> service.insuredValues(999L, 2018), ErrorCode.VALUATION_NOT_FOUND);
	}

	@ParameterizedTest
	@ValueSource(ints = { 2009, 2027, 1990 })
	void V3_yearOutOfRangeIsInvalidWithCurrentYearInMessage(int year) {
		assertThatThrownBy(() -> service.validateYear(year))
				.isInstanceOf(BusinessException.class)
				.hasMessage("El año debe estar entre 2010 y 2026")
				.extracting(ex -> ((BusinessException) ex).getCode())
				.isEqualTo(ErrorCode.INVALID_YEAR);
	}

	@ParameterizedTest
	@ValueSource(ints = { 2010, 2018, 2026 })
	void V3_yearBetweenMinimumAndCurrentYearIsValid(int year) {
		assertThatCode(() -> service.validateYear(year)).doesNotThrowAnyException();
	}

	@Test
	void V3_yearIsValidatedBeforeLookingUpTheValuation() {
		assertBusinessError(() -> service.insuredValues(999L, 2009), ErrorCode.INVALID_YEAR);
		verify(versionRepository, never()).findById(999L);
	}

	@Test
	void S5_rangeContainsItsBoundsInclusive() {
		InsuredValueRange range = service.calculate(new BigDecimal("18000000.00"), 2018);

		assertThat(range.contains(new BigDecimal("8640000.00"))).isTrue();
		assertThat(range.contains(new BigDecimal("12960000.00"))).isTrue();
		assertThat(range.contains(new BigDecimal("8639000.00"))).isFalse();
		assertThat(range.contains(new BigDecimal("12960000.01"))).isFalse();
	}

	private static VehicleVersion version(BigDecimal referenceValue) {
		VehicleVersion version = new VehicleVersion();
		version.setReferenceValue(referenceValue);
		return version;
	}

	private static void assertRange(InsuredValueRange range, String minimum, String suggested, String maximum) {
		assertThat(range.minimumAmount()).isEqualTo(new BigDecimal(minimum));
		assertThat(range.suggestedAmount()).isEqualTo(new BigDecimal(suggested));
		assertThat(range.maximumAmount()).isEqualTo(new BigDecimal(maximum));
	}

	private static void assertBusinessError(org.assertj.core.api.ThrowableAssert.ThrowingCallable call,
			ErrorCode code) {
		assertThatThrownBy(call)
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getCode())
				.isEqualTo(code);
	}

}
