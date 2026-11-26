package com.cotizador.dto;

import java.math.BigDecimal;

/** Tariff zone used in a calculation (api.md §8.2). */
public record ZoneResponse(String code, String name, BigDecimal factor) {
}
