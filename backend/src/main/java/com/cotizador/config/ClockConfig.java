package com.cotizador.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Business clock in the {@code app.timezone} zone (backend.md §3.1).
 * Services receive it by constructor; tests replace it with a fixed clock.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock(@Value("${app.timezone}") String timezone) {
		return Clock.system(ZoneId.of(timezone));
	}

}
