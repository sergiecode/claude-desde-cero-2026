package com.cotizador.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

/** Expiration on read (backend.md §7.2, rule Q3, manual §8.1). */
class QuoteExpirationTest {

	private static final OffsetDateTime VALID_UNTIL = OffsetDateTime.parse("2026-12-04T10:00:00-03:00");

	@Test
	void Q3_calculatedQuoteIsStillValidAtItsValidUntil() {
		Quote quote = quote(QuoteStatus.CALCULATED);

		assertThat(quote.expireIfOverdue(VALID_UNTIL)).isFalse();
		assertThat(quote.getStatus()).isEqualTo(QuoteStatus.CALCULATED);
	}

	@Test
	void Q3_calculatedQuoteExpiresOneSecondAfterItsValidUntil() {
		Quote quote = quote(QuoteStatus.CALCULATED);

		assertThat(quote.expireIfOverdue(VALID_UNTIL.plusSeconds(1))).isTrue();
		assertThat(quote.getStatus()).isEqualTo(QuoteStatus.EXPIRED);
	}

	@Test
	void Q3_comparesInstantsWhateverTheOffset() {
		Quote quote = quote(QuoteStatus.CALCULATED);

		// 2026-12-04T13:00:00Z is exactly the validUntil.
		assertThat(quote.expireIfOverdue(OffsetDateTime.parse("2026-12-04T13:00:00Z"))).isFalse();
		assertThat(quote.expireIfOverdue(OffsetDateTime.parse("2026-12-04T13:00:01Z"))).isTrue();
	}

	@Test
	void emittedQuoteNeverExpires() {
		Quote quote = quote(QuoteStatus.EMITTED);

		assertThat(quote.expireIfOverdue(VALID_UNTIL.plusYears(1))).isFalse();
		assertThat(quote.getStatus()).isEqualTo(QuoteStatus.EMITTED);
	}

	private static Quote quote(QuoteStatus status) {
		Quote quote = new Quote();
		quote.setStatus(status);
		quote.setValidUntil(VALID_UNTIL);
		return quote;
	}

}
