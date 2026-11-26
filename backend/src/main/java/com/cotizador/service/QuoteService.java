package com.cotizador.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.dto.QuoteCalculation;
import com.cotizador.dto.QuoteDetail;
import com.cotizador.dto.QuoteRequest;
import com.cotizador.dto.QuoteSummary;
import com.cotizador.entity.AllowedUsageId;
import com.cotizador.entity.Brand;
import com.cotizador.entity.Client;
import com.cotizador.entity.InsurancePlan;
import com.cotizador.entity.Quote;
import com.cotizador.entity.QuoteStatus;
import com.cotizador.entity.TariffZone;
import com.cotizador.entity.VehicleModel;
import com.cotizador.entity.VehicleVersion;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.QuoteCalculationMapper;
import com.cotizador.mapper.QuoteMapper;
import com.cotizador.repository.AllowedUsageRepository;
import com.cotizador.repository.BrandRepository;
import com.cotizador.repository.ClientRepository;
import com.cotizador.repository.InsurancePlanRepository;
import com.cotizador.repository.ProducerRepository;
import com.cotizador.repository.QuoteRepository;
import com.cotizador.repository.TariffZoneRepository;
import com.cotizador.repository.VehicleModelRepository;
import com.cotizador.repository.VehicleVersionRepository;
import com.cotizador.validation.PatentValidator;

/**
 * Quotes: price calculation (api.md §8.2), the business validations of backend.md §5.3, saving (backend.md §7.1)
 * and reading with expiration (backend.md §7.2). Reads are always filtered by producer in the query
 * (backend.md §7.4).
 */
@Service
public class QuoteService {

	/** Prefix of quote numbers (rule Q7). */
	static final String QUOTE_PREFIX = "COT";

	private final ClientRepository clientRepository;
	private final BrandRepository brandRepository;
	private final VehicleModelRepository modelRepository;
	private final VehicleVersionRepository versionRepository;
	private final AllowedUsageRepository allowedUsageRepository;
	private final InsurancePlanRepository planRepository;
	private final ClientService clientService;
	private final ValuationService valuationService;
	private final PricingService pricingService;
	private final PatentValidator patentValidator;
	private final QuoteCalculationMapper calculationMapper;
	private final QuoteRepository quoteRepository;
	private final ProducerRepository producerRepository;
	private final TariffZoneRepository tariffZoneRepository;
	private final NumberSequenceService numberSequenceService;
	private final QuoteMapper quoteMapper;
	private final PricingProperties pricingProperties;
	private final Clock clock;

	public QuoteService(ClientRepository clientRepository, BrandRepository brandRepository,
			VehicleModelRepository modelRepository, VehicleVersionRepository versionRepository,
			AllowedUsageRepository allowedUsageRepository, InsurancePlanRepository planRepository,
			ClientService clientService, ValuationService valuationService, PricingService pricingService,
			PatentValidator patentValidator, QuoteCalculationMapper calculationMapper, QuoteRepository quoteRepository,
			ProducerRepository producerRepository, TariffZoneRepository tariffZoneRepository,
			NumberSequenceService numberSequenceService, QuoteMapper quoteMapper, PricingProperties pricingProperties,
			Clock clock) {
		this.clientRepository = clientRepository;
		this.brandRepository = brandRepository;
		this.modelRepository = modelRepository;
		this.versionRepository = versionRepository;
		this.allowedUsageRepository = allowedUsageRepository;
		this.planRepository = planRepository;
		this.clientService = clientService;
		this.valuationService = valuationService;
		this.pricingService = pricingService;
		this.patentValidator = patentValidator;
		this.calculationMapper = calculationMapper;
		this.quoteRepository = quoteRepository;
		this.producerRepository = producerRepository;
		this.tariffZoneRepository = tariffZoneRepository;
		this.numberSequenceService = numberSequenceService;
		this.quoteMapper = quoteMapper;
		this.pricingProperties = pricingProperties;
		this.clock = clock;
	}

	/** Validates and calculates the price without saving anything (rule Q1). */
	@Transactional(readOnly = true)
	public QuoteCalculation calculate(QuoteRequest request) {
		return calculationMapper.toResponse(prepare(request).price());
	}

	/**
	 * Validates and recalculates the request (never uses a value calculated by the frontend), takes the next
	 * {@code COT} number and saves the producer's quote with a copy of every value used (rules Q2, Q3, Q6, Q7;
	 * backend.md §7.1). All in one transaction.
	 */
	@Transactional
	public QuoteDetail save(QuoteRequest request, Long producerId) {
		QuotePreparation preparation = prepare(request);
		PriceCalculation price = preparation.price();
		OffsetDateTime now = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);

		Quote quote = new Quote();
		// The year of the number is the year of created_at (backend.md §6).
		quote.setQuoteNumber(numberSequenceService.next(QUOTE_PREFIX, now.getYear()));
		quote.setProducer(producerRepository.getReferenceById(producerId));
		quote.setClient(preparation.client());
		quote.setVehicleType(request.vehicleType());
		quote.setManufacturingYear(request.manufacturingYear());
		quote.setBrand(preparation.brand());
		quote.setModel(preparation.model());
		quote.setVersion(preparation.version());
		quote.setBrandName(preparation.brand().getName());
		quote.setModelName(preparation.model().getName());
		quote.setVersionName(preparation.version().getName());
		quote.setPatent(preparation.patent());
		quote.setUsageType(request.usageType());
		quote.setPlan(preparation.plan());
		quote.setPlanName(preparation.plan().getName());
		quote.setPlanRate(price.planRate());
		quote.setInsuredAmount(price.insuredAmount());
		quote.setZoneCode(price.zoneCode());
		quote.setZoneFactor(price.zoneFactor());
		quote.setUsageFactor(price.usageFactor());
		quote.setVehicleAgeYears(price.vehicleAgeYears());
		quote.setVehicleAgeFactor(price.vehicleAgeFactor());
		quote.setDriverAge(price.driverAge());
		quote.setDriverAgeFactor(price.driverAgeFactor());
		quote.setBasePremium(price.basePremium());
		quote.setPremium(price.premium());
		quote.setTaxRate(price.taxRate());
		quote.setTaxes(price.taxes());
		quote.setTotalPrice(price.totalPrice());
		quote.setCurrency(QuoteCalculationMapper.CURRENCY);
		quote.setStatus(QuoteStatus.CALCULATED);
		quote.setCreatedAt(now);
		quote.setValidUntil(now.plusDays(pricingProperties.quoteValidityDays()));
		quoteRepository.save(quote);
		return toDetail(quote);
	}

	/**
	 * Quotes of the producer, most recent first (api.md §8.4). Expired ones are updated before answering
	 * (backend.md §7.2), so this method is not read-only.
	 */
	@Transactional
	public List<QuoteSummary> list(Long producerId) {
		OffsetDateTime now = OffsetDateTime.now(clock);
		List<Quote> quotes = quoteRepository.findByProducerIdOrderByCreatedAtDescIdDesc(producerId);
		quotes.forEach(quote -> quote.expireIfOverdue(now));
		return quotes.stream().map(quoteMapper::toSummary).toList();
	}

	/**
	 * A quote of the producer (api.md §8.5), with its expiration applied (backend.md §7.2). Not read-only because
	 * it may save the new status.
	 *
	 * @throws BusinessException with {@link ErrorCode#QUOTE_NOT_FOUND} if it does not exist or belongs to another
	 *         producer (rule Q5)
	 */
	@Transactional
	public QuoteDetail get(Long id, Long producerId) {
		Quote quote = quoteRepository.findByIdAndProducerId(id, producerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.QUOTE_NOT_FOUND));
		quote.expireIfOverdue(OffsetDateTime.now(clock));
		return toDetail(quote);
	}

	/** The zone name is not copied in the quote: it is read from {@code tariff_zones} by its code. */
	private QuoteDetail toDetail(Quote quote) {
		String zoneName = tariffZoneRepository.findByCode(quote.getZoneCode())
				.map(TariffZone::getName)
				.orElseThrow(() -> new IllegalStateException("Unknown tariff zone " + quote.getZoneCode()));
		// Issuing policies is stage 11; until then no quote has a policy.
		return quoteMapper.toDetail(quote, zoneName, null);
	}

	/**
	 * Runs the business validations of backend.md §5.3, in that order, and calculates the price. Never uses a
	 * value calculated by the frontend. Shared by {@code calculate} and saving a quote.
	 */
	@Transactional(readOnly = true)
	public QuotePreparation prepare(QuoteRequest request) {
		// 1 and 2. The client exists and, for a DNI, has the minimum age (rules C1 and C5).
		Client client = clientRepository.findWithCityById(request.clientId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
		clientService.ensureMinimumAge(client);

		// 3. The year is valid (rule V3).
		int year = request.manufacturingYear();
		valuationService.validateYear(year);

		// 4. The brand is of the type, the model is of the brand and available for the year, the version is of
		// the model (rule V4).
		Brand brand = brandRepository.findById(request.brandId())
				.filter(found -> found.getVehicleType() == request.vehicleType())
				.orElseThrow(QuoteService::invalidVehicle);
		VehicleModel model = modelRepository.findById(request.modelId())
				.filter(found -> Objects.equals(found.getBrand().getId(), brand.getId()))
				.filter(found -> found.getAvailableFromYear() <= year)
				.orElseThrow(QuoteService::invalidVehicle);
		VehicleVersion version = versionRepository.findById(request.versionId())
				.filter(found -> Objects.equals(found.getModel().getId(), model.getId()))
				.orElseThrow(QuoteService::invalidVehicle);

		// 5. The usage type is allowed for the vehicle type (rule V5).
		if (!allowedUsageRepository.existsById(new AllowedUsageId(request.vehicleType(), request.usageType()))) {
			throw new BusinessException(ErrorCode.USAGE_NOT_ALLOWED);
		}

		// 6. The patent has a valid format (rule V6).
		String patent = patentValidator.normalize(request.patent());

		// 7. The plan exists (rule P1).
		InsurancePlan plan = planRepository.findById(request.planId())
				.orElseThrow(() -> new BusinessException(ErrorCode.PLAN_NOT_FOUND));

		// 8. There is a valuation and the insured amount is within its range (rules S5 and S6).
		InsuredValueRange range = valuationService.rangeFor(version.getId(), year);
		BigDecimal insuredAmount = request.insuredAmount();
		if (!range.contains(insuredAmount)) {
			throw new BusinessException(ErrorCode.INSURED_AMOUNT_OUT_OF_RANGE,
					formatArs(range.minimumAmount()), formatArs(range.maximumAmount()));
		}

		int vehicleAgeYears = LocalDate.now(clock).getYear() - year;
		PriceCalculation price = pricingService.calculate(insuredAmount, plan.getMonthlyRate(),
				client.getCity().getZone(), request.usageType(), vehicleAgeYears, clientService.ageOf(client));
		return new QuotePreparation(client, brand, model, version, plan, patent, price);
	}

	private static BusinessException invalidVehicle() {
		return new BusinessException(ErrorCode.INVALID_VEHICLE);
	}

	/** Amount in es-AR format, without the currency sign (api.md §2.1): {@code 8.640.000,00}. */
	static String formatArs(BigDecimal amount) {
		DecimalFormat format = new DecimalFormat("#,##0.00", symbols());
		format.setRoundingMode(RoundingMode.HALF_UP);
		return format.format(amount);
	}

	private static DecimalFormatSymbols symbols() {
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator('.');
		symbols.setDecimalSeparator(',');
		return symbols;
	}

}
