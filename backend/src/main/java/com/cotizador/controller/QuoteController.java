package com.cotizador.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.QuoteCalculation;
import com.cotizador.dto.QuoteRequest;
import com.cotizador.service.QuoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Quote endpoints (api.md §8). */
@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Quotes", description = "Price calculation and quotes")
public class QuoteController {

	private final QuoteService quoteService;

	public QuoteController(QuoteService quoteService) {
		this.quoteService = quoteService;
	}

	@PostMapping("/calculate")
	@Operation(summary = "Calculate the price of a quote without saving it",
			description = "Validates the request (backend.md §5.3) and returns the price breakdown (manual §7). "
					+ "Nothing is saved (rule Q1).")
	@ApiResponse(responseCode = "200", description = "Price breakdown")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_YEAR, INVALID_VEHICLE, "
			+ "USAGE_NOT_ALLOWED, INVALID_PATENT, INSURED_AMOUNT_OUT_OF_RANGE or PLAN_NOT_FOUND")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "CLIENT_NOT_FOUND or VALUATION_NOT_FOUND")
	@ApiResponse(responseCode = "422", description = "CLIENT_UNDERAGE")
	public QuoteCalculation calculate(@Valid @RequestBody QuoteRequest request) {
		return quoteService.calculate(request);
	}

}
