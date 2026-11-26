package com.cotizador.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.dto.InsurancePlanResponse;
import com.cotizador.mapper.InsurancePlanMapper;
import com.cotizador.repository.InsurancePlanRepository;

/** Insurance plans (rules P1 and P2, manual §6.4; api.md §6). */
@Service
public class InsurancePlanService {

	private final InsurancePlanRepository insurancePlanRepository;
	private final InsurancePlanMapper insurancePlanMapper;

	public InsurancePlanService(InsurancePlanRepository insurancePlanRepository,
			InsurancePlanMapper insurancePlanMapper) {
		this.insurancePlanRepository = insurancePlanRepository;
		this.insurancePlanMapper = insurancePlanMapper;
	}

	/** All plans ordered by display order, without the monthly rate. */
	@Transactional(readOnly = true)
	public List<InsurancePlanResponse> findAll() {
		return insurancePlanRepository.findAllByOrderByDisplayOrderAsc().stream()
				.map(insurancePlanMapper::toResponse)
				.toList();
	}

}
