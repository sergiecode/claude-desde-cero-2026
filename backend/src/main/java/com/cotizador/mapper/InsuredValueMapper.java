package com.cotizador.mapper;

import org.springframework.stereotype.Component;

import com.cotizador.dto.InsuredValueResponse;
import com.cotizador.service.InsuredValueRange;

@Component
public class InsuredValueMapper {

	/** All amounts are in Argentine pesos. */
	static final String CURRENCY = "ARS";

	public InsuredValueResponse toResponse(InsuredValueRange range) {
		return new InsuredValueResponse(range.minimumAmount(), range.suggestedAmount(), range.maximumAmount(),
				CURRENCY);
	}

}
