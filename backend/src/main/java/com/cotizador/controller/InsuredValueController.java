package com.cotizador.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.InsuredValueResponse;
import com.cotizador.mapper.InsuredValueMapper;
import com.cotizador.service.ValuationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Insured amount endpoint (api.md §7). */
@RestController
@RequestMapping("/api/insured-values")
@Tag(name = "Insured values", description = "Suggested insured amount and allowed range")
public class InsuredValueController {

	private final ValuationService valuationService;
	private final InsuredValueMapper insuredValueMapper;

	public InsuredValueController(ValuationService valuationService, InsuredValueMapper insuredValueMapper) {
		this.valuationService = valuationService;
		this.insuredValueMapper = insuredValueMapper;
	}

	@GetMapping
	@Operation(summary = "Get the insured amount range",
			description = "Suggested amount from the 0 km value and the vehicle age, with minimum and maximum "
					+ "(rules S1 to S4, S6).")
	@ApiResponse(responseCode = "200", description = "Minimum, suggested and maximum amounts")
	@ApiResponse(responseCode = "400", description = "INVALID_YEAR or VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "VALUATION_NOT_FOUND")
	public InsuredValueResponse insuredValues(
			@Parameter(description = "Version id", example = "5") @RequestParam long versionId,
			@Parameter(description = "Manufacturing year", example = "2018") @RequestParam int year) {
		return insuredValueMapper.toResponse(valuationService.insuredValues(versionId, year));
	}

}
