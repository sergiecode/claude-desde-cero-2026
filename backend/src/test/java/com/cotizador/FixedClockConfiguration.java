package com.cotizador;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Fixed test clock: 2026-11-19T10:00:00-03:00 (backend.md §11). */
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockConfiguration {

	public static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-19T10:00:00-03:00");

	@Bean
	@Primary
	Clock fixedClock() {
		return Clock.fixed(NOW.toInstant(), ZoneId.of("America/Argentina/Buenos_Aires"));
	}

}
