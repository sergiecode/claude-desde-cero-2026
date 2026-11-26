package com.cotizador.dto;

/** An item of a vehicle catalog list: brand, model or version (api.md §5). */
public record CatalogItemResponse(Long id, String name) {
}
