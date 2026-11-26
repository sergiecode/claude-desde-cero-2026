package com.cotizador.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.InsurancePlanResponse;
import com.cotizador.service.InsurancePlanService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Insurance plan endpoints (api.md §6). */
@RestController
@RequestMapping("/api/insurance-plans")
@Tag(name = "Insurance plans", description = "Insurance plans")
public class InsurancePlanController {

	private final InsurancePlanService insurancePlanService;

	public InsurancePlanController(InsurancePlanService insurancePlanService) {
		this.insurancePlanService = insurancePlanService;
	}

	@GetMapping
	@Operation(summary = "List insurance plans",
			description = "Ordered by display order (rules P1, P2). The monthly rate is not exposed.")
	@ApiResponse(responseCode = "200", description = "Insurance plans")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<InsurancePlanResponse> findAll() {
		return insurancePlanService.findAll();
	}

}
