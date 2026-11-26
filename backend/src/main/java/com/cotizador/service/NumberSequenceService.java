package com.cotizador.service;

import java.time.Clock;
import java.time.Year;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.entity.NumberSequence;
import com.cotizador.repository.NumberSequenceRepository;

/**
 * Yearly correlative numbers {@code PREFIX-YYYY-NNNNNN} (backend.md §6, rules Q7 and E9). Runs inside the caller's
 * transaction: the counter row stays locked until it ends, so two quotes never get the same number.
 */
@Service
public class NumberSequenceService {

	/** Attempts of the locked select when another transaction creates the counter of the year at the same time. */
	static final int MAX_ATTEMPTS = 3;

	private final NumberSequenceRepository repository;
	private final Clock clock;

	public NumberSequenceService(NumberSequenceRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	/** Next number of the prefix for the current year according to the clock. */
	@Transactional(propagation = Propagation.MANDATORY)
	public String next(String prefix) {
		return next(prefix, Year.now(clock).getValue());
	}

	/**
	 * Next number of the prefix for the given year. Used when the year must match a date the caller already took
	 * from the clock (the {@code created_at} of a quote, the {@code issued_at} of a policy).
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public String next(String prefix, int year) {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			Optional<NumberSequence> found = repository.findByPrefixAndYear(prefix, year);
			if (found.isPresent()) {
				NumberSequence sequence = found.get();
				sequence.setLastValue(sequence.getLastValue() + 1);
				return format(prefix, year, sequence.getLastValue());
			}
			if (repository.insertFirst(prefix, year) == 1) {
				return format(prefix, year, 1);
			}
		}
		throw new IllegalStateException("Could not get the next number for " + prefix + "-" + year);
	}

	/** {@code COT-2026-000001}: prefix, year and the value with 6 digits padded with zeros. */
	static String format(String prefix, int year, int value) {
		return "%s-%d-%06d".formatted(prefix, year, value);
	}

}
