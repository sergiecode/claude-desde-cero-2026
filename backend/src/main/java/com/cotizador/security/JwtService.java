package com.cotizador.security;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.cotizador.config.JwtProperties;

/** Issues HS256 tokens whose {@code sub} is the producer id (backend.md §9). */
@Service
public class JwtService {

	private final JwtEncoder jwtEncoder;
	private final JwtProperties jwtProperties;
	private final Clock clock;

	public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties, Clock clock) {
		this.jwtEncoder = jwtEncoder;
		this.jwtProperties = jwtProperties;
		this.clock = clock;
	}

	public IssuedToken issue(Long producerId) {
		Instant issuedAt = clock.instant();
		Instant expiresAt = issuedAt.plus(jwtProperties.expiration());
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(String.valueOf(producerId))
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, OffsetDateTime.ofInstant(expiresAt, clock.getZone()));
	}

	/** A signed token and its expiration in the business time zone. */
	public record IssuedToken(String token, OffsetDateTime expiresAt) {
	}

}
