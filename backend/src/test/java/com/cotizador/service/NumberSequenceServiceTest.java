package com.cotizador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cotizador.entity.NumberSequence;
import com.cotizador.repository.NumberSequenceRepository;

class NumberSequenceServiceTest {

	private static final Clock CLOCK = Clock.fixed(OffsetDateTime.parse("2026-11-19T10:00:00-03:00").toInstant(),
			ZoneId.of("America/Argentina/Buenos_Aires"));

	private NumberSequenceRepository repository;
	private NumberSequenceService service;

	@BeforeEach
	void setUp() {
		repository = mock(NumberSequenceRepository.class);
		service = new NumberSequenceService(repository, CLOCK);
	}

	@Test
	void Q7_formatIsPrefixYearAndSixDigitsWithLeadingZeros() {
		assertThat(NumberSequenceService.format("COT", 2026, 1)).isEqualTo("COT-2026-000001");
		assertThat(NumberSequenceService.format("POL", 2027, 123456)).isEqualTo("POL-2027-123456");
		assertThat(NumberSequenceService.format("COT", 2026, 42)).isEqualTo("COT-2026-000042");
	}

	@Test
	void Q7_firstNumberOfTheYearCreatesTheCounterWithOne() {
		when(repository.findByPrefixAndYear("COT", 2026)).thenReturn(Optional.empty());
		when(repository.insertFirst("COT", 2026)).thenReturn(1);

		assertThat(service.next("COT")).isEqualTo("COT-2026-000001");
	}

	@Test
	void Q7_existingCounterIsIncremented() {
		NumberSequence sequence = sequence("COT", 2026, 7);
		when(repository.findByPrefixAndYear("COT", 2026)).thenReturn(Optional.of(sequence));

		assertThat(service.next("COT")).isEqualTo("COT-2026-000008");
		assertThat(sequence.getLastValue()).isEqualTo(8);
		verify(repository, never()).insertFirst("COT", 2026);
	}

	@Test
	void Q7_yearComesFromTheClockUnlessGiven() {
		when(repository.findByPrefixAndYear("POL", 2027)).thenReturn(Optional.empty());
		when(repository.insertFirst("POL", 2027)).thenReturn(1);

		assertThat(service.next("POL", 2027)).isEqualTo("POL-2027-000001");
	}

	@Test
	void counterCreatedByAnotherTransactionRetriesTheLockedSelect() {
		NumberSequence sequence = sequence("COT", 2026, 1);
		when(repository.findByPrefixAndYear("COT", 2026)).thenReturn(Optional.empty(), Optional.of(sequence));
		when(repository.insertFirst("COT", 2026)).thenReturn(0);

		assertThat(service.next("COT")).isEqualTo("COT-2026-000002");
		verify(repository, times(2)).findByPrefixAndYear("COT", 2026);
	}

	@Test
	void givesUpAfterTheMaximumAttempts() {
		when(repository.findByPrefixAndYear("COT", 2026)).thenReturn(Optional.empty());
		when(repository.insertFirst("COT", 2026)).thenReturn(0);

		assertThatThrownBy(() -> service.next("COT")).isInstanceOf(IllegalStateException.class);
		verify(repository, times(NumberSequenceService.MAX_ATTEMPTS)).findByPrefixAndYear("COT", 2026);
	}

	private static NumberSequence sequence(String prefix, int year, int lastValue) {
		NumberSequence sequence = new NumberSequence();
		sequence.setPrefix(prefix);
		sequence.setYear(year);
		sequence.setLastValue(lastValue);
		return sequence;
	}

}
