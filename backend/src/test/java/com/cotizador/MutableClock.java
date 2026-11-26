package com.cotizador;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test clock that stays fixed but can be moved to another instant, for criteria that need a date
 * other than the default {@link FixedClockConfiguration#NOW}.
 */
public class MutableClock extends Clock {

	private final AtomicReference<Instant> instant;
	private final ZoneId zone;

	public MutableClock(Instant instant, ZoneId zone) {
		this.instant = new AtomicReference<>(instant);
		this.zone = zone;
	}

	public void setTo(OffsetDateTime dateTime) {
		instant.set(dateTime.toInstant());
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId newZone) {
		return new MutableClock(instant.get(), newZone);
	}

	@Override
	public Instant instant() {
		return instant.get();
	}

}
