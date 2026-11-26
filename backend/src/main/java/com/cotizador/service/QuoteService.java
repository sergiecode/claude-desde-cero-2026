package com.cotizador.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.dto.QuoteCalculation;
import com.cotizador.dto.QuoteRequest;
import com.cotizador.entity.AllowedUsageId;
import com.cotizador.entity.Brand;
import com.cotizador.entity.Client;
import com.cotizador.entity.InsurancePlan;
import com.cotizador.entity.VehicleModel;
import com.cotizador.entity.VehicleVersion;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.QuoteCalculationMapper;
import com.cotizador.repository.AllowedUsageRepository;
import com.cotizador.repository.BrandRepository;
import com.cotizador.repository.ClientRepository;
import com.cotizador.repository.InsurancePlanRepository;
import com.cotizador.repository.VehicleModelRepository;
import com.cotizador.repository.VehicleVersionRepository;
import com.cotizador.validation.PatentValidator;

/** Quotes: price calculation (api.md §8.2) and the business validations of backend.md §5.3. */
@Service
public class QuoteService {

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
	private final Clock clock;

	public QuoteService(ClientRepository clientRepository, BrandRepository brandRepository,
			VehicleModelRepository modelRepository, VehicleVersionRepository versionRepository,
			AllowedUsageRepository allowedUsageRepository, InsurancePlanRepository planRepository,
			ClientService clientService, ValuationService valuationService, PricingService pricingService,
			PatentValidator patentValidator, QuoteCalculationMapper calculationMapper, Clock clock) {
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
		this.clock = clock;
	}

	/** Validates and calculates the price without saving anything (rule Q1). */
	@Transactional(readOnly = true)
	public QuoteCalculation calculate(QuoteRequest request) {
		return calculationMapper.toResponse(prepare(request).price());
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
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator('.');
		symbols.setDecimalSeparator(',');
		DecimalFormat format = new DecimalFormat("#,##0.00", symbols);
		format.setRoundingMode(RoundingMode.HALF_UP);
		return format.format(amount);
	}

}
