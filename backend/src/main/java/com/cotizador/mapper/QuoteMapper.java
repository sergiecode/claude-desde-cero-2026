package com.cotizador.mapper;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

import com.cotizador.dto.QuoteDetail;
import com.cotizador.dto.QuotePolicy;
import com.cotizador.dto.QuoteSummary;
import com.cotizador.dto.QuoteVehicle;
import com.cotizador.entity.Client;
import com.cotizador.entity.DocumentType;
import com.cotizador.entity.Quote;
import com.cotizador.service.PriceCalculation;

/** Saved quotes to their responses (api.md §8.4 and §8.5), always with the values copied in the quote (rule Q2). */
@Component
public class QuoteMapper {

	private final ClientMapper clientMapper;
	private final QuoteCalculationMapper calculationMapper;
	private final Clock clock;

	public QuoteMapper(ClientMapper clientMapper, QuoteCalculationMapper calculationMapper, Clock clock) {
		this.clientMapper = clientMapper;
		this.calculationMapper = calculationMapper;
		this.clock = clock;
	}

	public QuoteSummary toSummary(Quote quote) {
		return new QuoteSummary(
				quote.getId(),
				quote.getQuoteNumber(),
				inBusinessZone(quote.getCreatedAt()),
				clientName(quote.getClient()),
				quote.getVehicleType(),
				quote.getBrandName(),
				quote.getModelName(),
				quote.getPlanName(),
				quote.getTotalPrice(),
				quote.getCurrency(),
				quote.getStatus());
	}

	/**
	 * @param zoneName name of the tariff zone, read from {@code tariff_zones} by the stored zone code (it is not
	 *        copied in the quote)
	 * @param policy the policy issued from the quote, or null
	 */
	public QuoteDetail toDetail(Quote quote, String zoneName, QuotePolicy policy) {
		return new QuoteDetail(
				quote.getId(),
				quote.getQuoteNumber(),
				quote.getStatus(),
				inBusinessZone(quote.getCreatedAt()),
				inBusinessZone(quote.getValidUntil()),
				clientMapper.toResponse(quote.getClient(), quote.getDriverAge()),
				new QuoteVehicle(
						quote.getVehicleType(),
						quote.getManufacturingYear(),
						quote.getBrandName(),
						quote.getModelName(),
						quote.getVersionName(),
						quote.getPatent(),
						quote.getUsageType()),
				quote.getPlanName(),
				calculationMapper.toResponse(new PriceCalculation(
						quote.getInsuredAmount(),
						quote.getPlanRate(),
						quote.getBasePremium(),
						quote.getZoneCode(),
						zoneName,
						quote.getZoneFactor(),
						quote.getUsageFactor(),
						quote.getVehicleAgeYears(),
						quote.getVehicleAgeFactor(),
						quote.getDriverAge(),
						quote.getDriverAgeFactor(),
						quote.getPremium(),
						quote.getTaxRate(),
						quote.getTaxes(),
						quote.getTotalPrice())),
				policy);
	}

	/** "First Last" for a person (DNI), the business name for a company (CUIT). */
	static String clientName(Client client) {
		return client.getDocumentType() == DocumentType.DNI
				? client.getFirstName() + " " + client.getLastName()
				: client.getBusinessName();
	}

	/** The database may return another offset; responses use the business zone ({@code -03:00}). */
	private OffsetDateTime inBusinessZone(OffsetDateTime dateTime) {
		return dateTime.atZoneSameInstant(clock.getZone()).toOffsetDateTime();
	}

}
