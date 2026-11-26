package com.cotizador.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.CatalogItemResponse;
import com.cotizador.entity.VehicleType;
import com.cotizador.entity.VehicleUsageType;
import com.cotizador.service.CatalogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Vehicle catalog endpoints (api.md §5). All of them return 200 with a list, empty if nothing matches. */
@RestController
@RequestMapping("/api/catalogs")
@Tag(name = "Catalogs", description = "Vehicle catalogs")
public class CatalogController {

	private final CatalogService catalogService;

	public CatalogController(CatalogService catalogService) {
		this.catalogService = catalogService;
	}

	@GetMapping("/vehicle-types")
	@Operation(summary = "List vehicle types", description = "In enum order.")
	@ApiResponse(responseCode = "200", description = "Vehicle types")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<VehicleType> vehicleTypes() {
		return catalogService.vehicleTypes();
	}

	@GetMapping("/years")
	@Operation(summary = "List vehicle years", description = "From the current year down to 2010 (rule V3).")
	@ApiResponse(responseCode = "200", description = "Years, descending")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<Integer> years(
			@Parameter(description = "Vehicle type", example = "AUTO") @RequestParam VehicleType vehicleType) {
		return catalogService.years(vehicleType);
	}

	@GetMapping("/brands")
	@Operation(summary = "List brands",
			description = "Brands of the type with at least one model available for the year, ordered by name "
					+ "(rule V4).")
	@ApiResponse(responseCode = "200", description = "Brands, empty if none")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<CatalogItemResponse> brands(
			@Parameter(description = "Vehicle type", example = "AUTO") @RequestParam VehicleType vehicleType,
			@Parameter(description = "Vehicle year", example = "2020") @RequestParam int year) {
		return catalogService.brands(vehicleType, year);
	}

	@GetMapping("/models")
	@Operation(summary = "List models",
			description = "Models of the brand (which must be of the type) available for the year, ordered by "
					+ "name (rule V4).")
	@ApiResponse(responseCode = "200", description = "Models, empty if none")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<CatalogItemResponse> models(
			@Parameter(description = "Vehicle type", example = "AUTO") @RequestParam VehicleType vehicleType,
			@Parameter(description = "Vehicle year", example = "2020") @RequestParam int year,
			@Parameter(description = "Brand id", example = "2") @RequestParam long brandId) {
		return catalogService.models(vehicleType, year, brandId);
	}

	@GetMapping("/versions")
	@Operation(summary = "List versions", description = "Versions of the model, ordered by id (rule V4).")
	@ApiResponse(responseCode = "200", description = "Versions, empty if none")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<CatalogItemResponse> versions(
			@Parameter(description = "Model id", example = "3") @RequestParam long modelId) {
		return catalogService.versions(modelId);
	}

	@GetMapping("/usage-types")
	@Operation(summary = "List usage types",
			description = "Usage types allowed for the vehicle type, in enum order (rule V5).")
	@ApiResponse(responseCode = "200", description = "Usage types")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<VehicleUsageType> usageTypes(
			@Parameter(description = "Vehicle type", example = "AUTO") @RequestParam VehicleType vehicleType) {
		return catalogService.usageTypes(vehicleType);
	}

}
