package com.cotizador.dto;

import java.time.OffsetDateTime;

public record LoginResponse(String token, OffsetDateTime expiresAt, ProducerResponse producer) {
}
