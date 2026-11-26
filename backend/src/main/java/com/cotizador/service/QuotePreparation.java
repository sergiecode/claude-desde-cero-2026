package com.cotizador.service;

import com.cotizador.entity.Brand;
import com.cotizador.entity.Client;
import com.cotizador.entity.InsurancePlan;
import com.cotizador.entity.VehicleModel;
import com.cotizador.entity.VehicleVersion;

/**
 * A quote request that passed every business validation (backend.md §5.3), with the entities it refers to, the
 * normalized patent and the price. Shared by calculating and saving a quote.
 */
public record QuotePreparation(
		Client client,
		Brand brand,
		VehicleModel model,
		VehicleVersion version,
		InsurancePlan plan,
		String patent,
		PriceCalculation price) {
}
