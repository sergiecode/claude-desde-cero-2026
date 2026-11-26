package com.cotizador.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.dto.PolicyDetail;
import com.cotizador.entity.Policy;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.PolicyMapper;
import com.cotizador.repository.PolicyRepository;

/**
 * Issuing (backend.md §7.3) and reading (api.md §9.1) policies. Reads are always filtered by producer in the
 * query (backend.md §7.4, rule E10).
 */
@Service
public class PolicyService {

	private final QuoteExpirationService quoteExpirationService;
	private final PolicyIssuer policyIssuer;
	private final PolicyRepository policyRepository;
	private final PolicyMapper policyMapper;
	private final Clock clock;

	public PolicyService(QuoteExpirationService quoteExpirationService, PolicyIssuer policyIssuer,
			PolicyRepository policyRepository, PolicyMapper policyMapper, Clock clock) {
		this.quoteExpirationService = quoteExpirationService;
		this.policyIssuer = policyIssuer;
		this.policyRepository = policyRepository;
		this.policyMapper = policyMapper;
		this.clock = clock;
	}

	/**
	 * Issues the policy of a quote of the producer.
	 * <p>
	 * Not transactional on purpose: it runs two transactions one after the other. First the expiration of the quote
	 * is saved in its own transaction ({@link QuoteExpirationService}), so an overdue quote stays EXPIRED even
	 * though the issue fails (rule E3, manual §8.1). Only then the issue transaction locks the quote row
	 * ({@link PolicyIssuer}). In that order no transaction ever waits for a lock held by a suspended one.
	 *
	 * @throws BusinessException {@code QUOTE_NOT_FOUND}, {@code QUOTE_EXPIRED}, {@code QUOTE_ALREADY_ISSUED} or
	 *         {@code INVALID_START_DATE}
	 */
	public PolicyDetail issue(Long quoteId, LocalDate startDate, Long producerId) {
		OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
		if (quoteExpirationService.expireIfOverdue(quoteId, producerId, now)) {
			throw new BusinessException(ErrorCode.QUOTE_EXPIRED);
		}
		try {
			return policyIssuer.issue(quoteId, startDate, producerId, now);
		}
		catch (DataIntegrityViolationException ex) {
			// Second line of defense of rule E2: the UNIQUE (quote_id) of policies (backend.md §7.3).
			throw policyIssuer.alreadyIssued(quoteId, producerId);
		}
	}

	/**
	 * A policy of the producer (api.md §9.1), with its expiration applied (backend.md §7.2). Not read-only because
	 * it may save the new status.
	 *
	 * @throws BusinessException with {@link ErrorCode#POLICY_NOT_FOUND} if it does not exist or belongs to another
	 *         producer (rule E10)
	 */
	@Transactional
	public PolicyDetail get(Long id, Long producerId) {
		Policy policy = policyRepository.findByIdAndProducerId(id, producerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.POLICY_NOT_FOUND));
		policy.expireIfOverdue(LocalDate.now(clock));
		return policyMapper.toDetail(policy);
	}

}
