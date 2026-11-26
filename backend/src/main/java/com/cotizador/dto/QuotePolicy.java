package com.cotizador.dto;

/** Policy issued from a quote (api.md §8.5). */
public record QuotePolicy(Long id, String policyNumber) {
}
