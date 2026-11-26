package com.cotizador.service;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.entity.QuoteStatus;
import com.cotizador.repository.QuoteRepository;

/**
 * Expiration of a quote when it is issued (backend.md §7.2, rule E3), saved in its own transaction so the EXPIRED
 * status stays in the database even though the issue then fails with {@code QUOTE_EXPIRED}.
 * <p>
 * It is a separate bean so the call from {@link PolicyService} goes through the proxy and really opens a new
 * transaction. It runs before the issue transaction locks the quote row: a {@code REQUIRES_NEW} update of a row
 * locked by a suspended outer transaction would wait for that lock forever.
 */
@Service
public class QuoteExpirationService {

	private final QuoteRepository quoteRepository;

	public QuoteExpirationService(QuoteRepository quoteRepository) {
		this.quoteRepository = quoteRepository;
	}

	/**
	 * Changes the producer's quote to EXPIRED if it is CALCULATED and {@code now} is after its {@code valid_until},
	 * and commits. Does nothing when the quote does not exist, belongs to another producer or is in another status.
	 *
	 * @return true when this call expired the quote
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean expireIfOverdue(Long quoteId, Long producerId, OffsetDateTime now) {
		return quoteRepository.expireIfOverdue(quoteId, producerId, now, QuoteStatus.CALCULATED,
				QuoteStatus.EXPIRED) > 0;
	}

}
