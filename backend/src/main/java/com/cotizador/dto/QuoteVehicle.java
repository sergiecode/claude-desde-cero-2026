package com.cotizador.dto;

import com.cotizador.entity.VehicleType;
import com.cotizador.entity.VehicleUsageType;

/** Vehicle of a saved quote (api.md §8.5), with the names copied when it was saved. */
public record QuoteVehicle(
		VehicleType vehicleType,
		Integer manufacturingYear,
		String brandName,
		String modelName,
		String versionName,
		String patent,
		VehicleUsageType usageType) {
}
