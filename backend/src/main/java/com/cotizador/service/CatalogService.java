package com.cotizador.service;

import java.time.Clock;
import java.time.Year;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.config.PricingProperties;
import com.cotizador.dto.CatalogItemResponse;
import com.cotizador.entity.AllowedUsage;
import com.cotizador.entity.VehicleType;
import com.cotizador.entity.VehicleUsageType;
import com.cotizador.mapper.CatalogMapper;
import com.cotizador.repository.AllowedUsageRepository;
import com.cotizador.repository.BrandRepository;
import com.cotizador.repository.VehicleModelRepository;
import com.cotizador.repository.VehicleVersionRepository;

/** Vehicle catalogs with the filtering rules V3, V4 and V5 (manual §6.2, api.md §5). */
@Service
public class CatalogService {

	private final BrandRepository brandRepository;
	private final VehicleModelRepository modelRepository;
	private final VehicleVersionRepository versionRepository;
	private final AllowedUsageRepository allowedUsageRepository;
	private final CatalogMapper catalogMapper;
	private final PricingProperties pricingProperties;
	private final Clock clock;

	public CatalogService(BrandRepository brandRepository, VehicleModelRepository modelRepository,
			VehicleVersionRepository versionRepository, AllowedUsageRepository allowedUsageRepository,
			CatalogMapper catalogMapper, PricingProperties pricingProperties, Clock clock) {
		this.brandRepository = brandRepository;
		this.modelRepository = modelRepository;
		this.versionRepository = versionRepository;
		this.allowedUsageRepository = allowedUsageRepository;
		this.catalogMapper = catalogMapper;
		this.pricingProperties = pricingProperties;
		this.clock = clock;
	}

	/** All vehicle types, in enum order. */
	public List<VehicleType> vehicleTypes() {
		return Arrays.asList(VehicleType.values());
	}

	/**
	 * Rule V3: from the current year of the business clock down to {@code app.pricing.min-vehicle-year},
	 * descending. The range is the same for every vehicle type.
	 */
	public List<Integer> years(VehicleType vehicleType) {
		int currentYear = Year.now(clock).getValue();
		int minYear = pricingProperties.minVehicleYear();
		return IntStream.iterate(currentYear, year -> year >= minYear, year -> year - 1)
				.boxed()
				.toList();
	}

	/** Rule V4: brands of the type with at least one model available for the year, ordered by name. */
	@Transactional(readOnly = true)
	public List<CatalogItemResponse> brands(VehicleType vehicleType, int year) {
		return brandRepository.findAvailable(vehicleType, year).stream()
				.map(catalogMapper::toResponse)
				.toList();
	}

	/** Rule V4: models of the brand (which must be of the type) available for the year, ordered by name. */
	@Transactional(readOnly = true)
	public List<CatalogItemResponse> models(VehicleType vehicleType, int year, long brandId) {
		return modelRepository.findAvailable(vehicleType, year, brandId).stream()
				.map(catalogMapper::toResponse)
				.toList();
	}

	/** Rule V4: versions of the model, ordered by id. */
	@Transactional(readOnly = true)
	public List<CatalogItemResponse> versions(long modelId) {
		return versionRepository.findByModelIdOrderByIdAsc(modelId).stream()
				.map(catalogMapper::toResponse)
				.toList();
	}

	/** Rule V5: usage types allowed for the vehicle type, in enum order. */
	@Transactional(readOnly = true)
	public List<VehicleUsageType> usageTypes(VehicleType vehicleType) {
		return allowedUsageRepository.findByVehicleType(vehicleType).stream()
				.map(AllowedUsage::getUsageType)
				.sorted(Comparator.naturalOrder())
				.toList();
	}

}
