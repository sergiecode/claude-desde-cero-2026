package com.cotizador.mapper;

import java.time.Clock;

import org.springframework.stereotype.Component;

import com.cotizador.dto.PolicyDetail;
import com.cotizador.dto.QuotePolicy;
import com.cotizador.entity.Policy;
import com.cotizador.entity.Quote;

/** Policies to their responses (api.md §9.1 and the {@code policy} of api.md §8.5). */
@Component
public class PolicyMapper {

	private final ClientMapper clientMapper;
	private final QuoteMapper quoteMapper;
	private final Clock clock;

	public PolicyMapper(ClientMapper clientMapper, QuoteMapper quoteMapper, Clock clock) {
		this.clientMapper = clientMapper;
		this.quoteMapper = quoteMapper;
		this.clock = clock;
	}

	/** The client's age is the one stored in the quote when it was quoted, as in the quote detail. */
	public PolicyDetail toDetail(Policy policy) {
		Quote quote = policy.getQuote();
		return new PolicyDetail(
				policy.getId(),
				policy.getPolicyNumber(),
				policy.getStatus(),
				policy.getIssuedAt().atZoneSameInstant(clock.getZone()).toOffsetDateTime(),
				policy.getStartDate(),
				policy.getEndDate(),
				quote.getId(),
				quote.getQuoteNumber(),
				clientMapper.toResponse(policy.getClient(), quote.getDriverAge()),
				quoteMapper.toVehicle(quote),
				quote.getPlanName(),
				policy.getInsuredAmount(),
				policy.getPremium(),
				policy.getTaxes(),
				policy.getTotalPrice(),
				policy.getCurrency());
	}

	public QuotePolicy toQuotePolicy(Policy policy) {
		return new QuotePolicy(policy.getId(), policy.getPolicyNumber());
	}

}
