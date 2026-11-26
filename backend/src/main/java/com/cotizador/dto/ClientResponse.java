package com.cotizador.dto;

import java.time.LocalDate;

import com.cotizador.entity.DocumentType;

/**
 * Client search result (api.md §4). For a company (CUIT), {@code firstName}, {@code lastName},
 * {@code birthDate} and {@code age} are null and {@code businessName} has a value.
 */
public record ClientResponse(
		Long id,
		DocumentType documentType,
		String documentNumber,
		String firstName,
		String lastName,
		String businessName,
		LocalDate birthDate,
		Integer age,
		String email,
		String phone,
		String address,
		String postalCode,
		String city,
		String province) {
}
