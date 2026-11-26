package com.cotizador.mapper;

import org.springframework.stereotype.Component;

import com.cotizador.dto.CatalogItemResponse;
import com.cotizador.entity.Brand;
import com.cotizador.entity.VehicleModel;
import com.cotizador.entity.VehicleVersion;

@Component
public class CatalogMapper {

	public CatalogItemResponse toResponse(Brand brand) {
		return new CatalogItemResponse(brand.getId(), brand.getName());
	}

	public CatalogItemResponse toResponse(VehicleModel model) {
		return new CatalogItemResponse(model.getId(), model.getName());
	}

	public CatalogItemResponse toResponse(VehicleVersion version) {
		return new CatalogItemResponse(version.getId(), version.getName());
	}

}
