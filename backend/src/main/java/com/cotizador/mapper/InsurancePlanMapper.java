package com.cotizador.mapper;

import org.springframework.stereotype.Component;

import com.cotizador.dto.InsurancePlanResponse;
import com.cotizador.entity.InsurancePlan;

@Component
public class InsurancePlanMapper {

	public InsurancePlanResponse toResponse(InsurancePlan plan) {
		return new InsurancePlanResponse(plan.getId(), plan.getCode(), plan.getName(), plan.getDescription());
	}

}
