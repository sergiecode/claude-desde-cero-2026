package com.cotizador.dto;

/** An insurance plan (api.md §6). The monthly rate is intentionally not exposed. */
public record InsurancePlanResponse(Long id, String code, String name, String description) {
}
