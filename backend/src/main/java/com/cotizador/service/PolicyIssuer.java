package com.cotizador.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.dto.PolicyDetail;
import com.cotizador.entity.Policy;
import com.cotizador.entity.PolicyStatus;
import com.cotizador.entity.Quote;
import com.cotizador.entity.QuoteStatus;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.PolicyMapper;
import com.cotizador.repository.PolicyRepository;
import com.cotizador.repository.QuoteRepository;

/**
 * The issue transaction of backend.md §7.3, with the quote row locked. Called only by {@link PolicyService}, after
 * the expiration was applied and saved in its own transaction ({@link QuoteExpirationService}).
 */
@Service
public class PolicyIssuer {

	/** Prefix of policy numbers (rule E9). */
	static final String POLICY_PREFIX = "POL";

	private final QuoteRepository quoteRepository;
	private final PolicyRepository policyRepository;
	private final NumberSequenceService numberSequenceService;
	private final PolicyMapper policyMapper;
	private final PricingProperties pricingProperties;

	public PolicyIssuer(QuoteRepository quoteRepository, PolicyRepository policyRepository,
			NumberSequenceService numberSequenceService, PolicyMapper policyMapper,
			PricingProperties pricingProperties) {
		this.quoteRepository = quoteRepository;
		this.policyRepository = policyRepository;
		this.numberSequenceService = numberSequenceService;
		this.policyMapper = policyMapper;
		this.pricingProperties = pricingProperties;
	}

	/**
	 * Issues the policy of the producer's quote, in one transaction.
	 *
	 * @param now the current time, taken once by the caller from the clock
	 */
	@Transactional
	public PolicyDetail issue(Long quoteId, LocalDate startDate, Long producerId, OffsetDateTime now) {
		// 1. The quote of the producer, locked until the transaction ends (rules E2 and E10).
		Quote quote = quoteRepository.lockByIdAndProducerId(quoteId, producerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_NOT_FOUND));

		// 2. Expiration. It was already applied and saved in its own transaction before taking the lock; if the
		// quote expired in between, the issue is refused all the same (rule E3).
		if (quote.getStatus() == QuoteStatus.CALCULATED && now.isAfter(quote.getValidUntil())) {
			throw new BusinessException(ErrorCode.QUOTE_EXPIRED);
		}

		// 3 and 4. Only a CALCULATED quote is issued (rules E1, E2 and E3).
		if (quote.getStatus() == QuoteStatus.EMITTED) {
			throw alreadyIssued(quoteId, producerId);
		}
		if (quote.getStatus() == QuoteStatus.EXPIRED) {
			throw new BusinessException(ErrorCode.QUOTE_EXPIRED);
		}

		// 5. Today or up to 30 days ahead (rule E4).
		LocalDate today = now.toLocalDate();
		LocalDate lastAllowed = today.plusDays(pricingProperties.maxStartDateDaysAhead());
		if (startDate.isBefore(today) || startDate.isAfter(lastAllowed)) {
			throw BusinessException.forField(ErrorCode.INVALID_START_DATE, "startDate");
		}

		// 6 and 7. The year of the number is the year of issued_at (backend.md §6). Amounts copied (rule E6).
		Policy policy = new Policy();
		policy.setPolicyNumber(numberSequenceService.next(POLICY_PREFIX, now.getYear()));
		policy.setQuote(quote);
		policy.setProducer(quote.getProducer());
		policy.setClient(quote.getClient());
		policy.setStartDate(startDate);
		policy.setEndDate(startDate.plusYears(1));
		policy.setInsuredAmount(quote.getInsuredAmount());
		policy.setPremium(quote.getPremium());
		policy.setTaxes(quote.getTaxes());
		policy.setTotalPrice(quote.getTotalPrice());
		policy.setCurrency(quote.getCurrency());
		policy.setStatus(PolicyStatus.ACTIVE);
		policy.setIssuedAt(now);
		policyRepository.save(policy);

		// 8. The quote is EMITTED (rule E8).
		quote.setStatus(QuoteStatus.EMITTED);
		return policyMapper.toDetail(policy);
	}

	/**
	 * {@code QUOTE_ALREADY_ISSUED} with the number of the existing policy (rule E2). Reads the policy in the
	 * caller's transaction, or in its own one when there is none.
	 */
	@Transactional(readOnly = true)
	public BusinessException alreadyIssued(Long quoteId, Long producerId) {
		String policyNumber = policyRepository.findByQuoteIdAndProducerId(quoteId, producerId)
				.map(Policy::getPolicyNumber)
				.orElseThrow(() -> new IllegalStateException("Quote " + quoteId + " is issued without a policy"));
		return new BusinessException(ErrorCode.QUOTE_ALREADY_ISSUED, policyNumber);
	}

}
