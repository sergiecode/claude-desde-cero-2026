package com.cotizador.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.IssueRequest;
import com.cotizador.dto.PolicyDetail;
import com.cotizador.security.CurrentProducer;
import com.cotizador.service.PolicyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Policy endpoints: issue a quote (api.md §8.6) and read a policy (api.md §9.1). */
@RestController
@Tag(name = "Policies", description = "Issuing and reading policies")
public class PolicyController {

	private final PolicyService policyService;
	private final CurrentProducer currentProducer;

	public PolicyController(PolicyService policyService, CurrentProducer currentProducer) {
		this.policyService = policyService;
		this.currentProducer = currentProducer;
	}

	@PostMapping("/api/quotes/{id}/issue")
	@Operation(summary = "Issue the policy of a quote",
			description = "Only a CALCULATED quote is issued, at most once. The start date is today or up to 30 "
					+ "days ahead; the policy lasts one year and keeps the amounts of the quote (rules E1 to E9).")
	@ApiResponse(responseCode = "201", description = "Policy issued; Location header points to it")
	@ApiResponse(responseCode = "400", description = "VALIDATION_ERROR or INVALID_START_DATE")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "QUOTE_NOT_FOUND (does not exist or belongs to another producer)")
	@ApiResponse(responseCode = "409", description = "QUOTE_EXPIRED or QUOTE_ALREADY_ISSUED")
	public ResponseEntity<PolicyDetail> issue(@PathVariable Long id, @Valid @RequestBody IssueRequest request) {
		PolicyDetail policy = policyService.issue(id, request.startDate(), currentProducer.id());
		return ResponseEntity.created(URI.create("/api/policies/" + policy.id())).body(policy);
	}

	@GetMapping("/api/policies/{id}")
	@Operation(summary = "Get a policy of the logged-in producer",
			description = "An ACTIVE policy past its end date is updated to EXPIRED before answering (manual §8.2).")
	@ApiResponse(responseCode = "200", description = "Policy detail")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "POLICY_NOT_FOUND (does not exist or belongs to another producer)")
	public PolicyDetail get(@PathVariable Long id) {
		return policyService.get(id, currentProducer.id());
	}

}
