package com.cotizador.controller;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.QuoteCalculation;
import com.cotizador.dto.QuoteDetail;
import com.cotizador.dto.QuoteRequest;
import com.cotizador.dto.QuoteSummary;
import com.cotizador.security.CurrentProducer;
import com.cotizador.service.QuoteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Quote endpoints (api.md §8). The producer always comes from the token. */
@RestController
@RequestMapping("/api/quotes")
@Tag(name = "Quotes", description = "Price calculation and quotes")
public class QuoteController {

	private final QuoteService quoteService;
	private final CurrentProducer currentProducer;

	public QuoteController(QuoteService quoteService, CurrentProducer currentProducer) {
		this.quoteService = quoteService;
		this.currentProducer = currentProducer;
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

	@PostMapping
	@Operation(summary = "Save a quote",
			description = "Recalculates with the same data (no value from the client is trusted) and saves the "
					+ "quote as CALCULATED, valid for 15 days (rules Q2, Q3, Q7).")
	@ApiResponse(responseCode = "201", description = "Quote saved; Location header points to it")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_YEAR, INVALID_VEHICLE, "
			+ "USAGE_NOT_ALLOWED, INVALID_PATENT, INSURED_AMOUNT_OUT_OF_RANGE or PLAN_NOT_FOUND")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "CLIENT_NOT_FOUND or VALUATION_NOT_FOUND")
	@ApiResponse(responseCode = "422", description = "CLIENT_UNDERAGE")
	public ResponseEntity<QuoteDetail> save(@Valid @RequestBody QuoteRequest request) {
		QuoteDetail quote = quoteService.save(request, currentProducer.id());
		return ResponseEntity.created(URI.create("/api/quotes/" + quote.id())).body(quote);
	}

	@GetMapping
	@Operation(summary = "List the quotes of the logged-in producer",
			description = "Most recent first. Expired quotes are updated before answering (rules Q3, Q5).")
	@ApiResponse(responseCode = "200", description = "Quotes of the producer")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	public List<QuoteSummary> list() {
		return quoteService.list(currentProducer.id());
	}

	@GetMapping("/{id}")
	@Operation(summary = "Get a quote of the logged-in producer",
			description = "The client's age is the one stored when quoting. Expiration is applied (rule Q3).")
	@ApiResponse(responseCode = "200", description = "Quote detail")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "QUOTE_NOT_FOUND (does not exist or belongs to another producer)")
	public QuoteDetail get(@PathVariable Long id) {
		return quoteService.get(id, currentProducer.id());
	}

}
