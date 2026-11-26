package com.cotizador.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.ClientResponse;
import com.cotizador.entity.DocumentType;
import com.cotizador.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Client endpoints (api.md §4). */
@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client search")
public class ClientController {

	private final ClientService clientService;

	public ClientController(ClientService clientService) {
		this.clientService = clientService;
	}

	@GetMapping("/search")
	@Operation(summary = "Search a client by document",
			description = "The CUIT is accepted with or without dashes. For a company, firstName, lastName, "
					+ "birthDate and age are null.")
	@ApiResponse(responseCode = "200", description = "Client found")
	@ApiResponse(responseCode = "400", description = "INVALID_DOCUMENT or VALIDATION_ERROR")
	@ApiResponse(responseCode = "401", description = "UNAUTHORIZED")
	@ApiResponse(responseCode = "404", description = "CLIENT_NOT_FOUND")
	@ApiResponse(responseCode = "422", description = "CLIENT_UNDERAGE")
	public ClientResponse search(
			@Parameter(description = "DNI or CUIT", example = "DNI") @RequestParam DocumentType documentType,
			@Parameter(description = "Document number", example = "30123456") @RequestParam String documentNumber) {
		return clientService.search(documentType, documentNumber);
	}

}
