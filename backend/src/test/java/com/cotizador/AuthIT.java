package com.cotizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Stage 3: login, session and 401 handling (AC-AUTH-01 to AC-AUTH-05). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class AuthIT {

	private static final String ANA_EMAIL = "ana.torres@aseguradora.test";
	private static final String PASSWORD = "Demo2026!";
	private static final String INVALID_CREDENTIALS_DETAIL = "Usuario o contraseña incorrectos";
	private static final String UNAUTHORIZED_DETAIL = "La sesión expiró. Ingresá nuevamente";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Autowired
	private JwtDecoder jwtDecoder;

	@Test
	void AC_AUTH_01_loginWithValidCredentialsReturnsTokenAndProducer() throws Exception {
		String body = login(ANA_EMAIL, PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.expiresAt").value("2026-11-19T18:00:00-03:00"))
				.andExpect(jsonPath("$.producer.id").value(1))
				.andExpect(jsonPath("$.producer.firstName").value("Ana"))
				.andExpect(jsonPath("$.producer.lastName").value("Torres"))
				.andExpect(jsonPath("$.producer.email").value(ANA_EMAIL))
				.andReturn().getResponse().getContentAsString();

		String token = JsonPath.read(body, "$.token");
		var jwt = jwtDecoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo("1");
		assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
		assertThat(jwt.getExpiresAt()).isEqualTo(Instant.parse("2026-11-19T21:00:00Z"));
	}

	@Test
	void AC_AUTH_02_loginEmailIsCaseInsensitive() throws Exception {
		login("ANA.TORRES@aseguradora.test", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.producer.id").value(1))
				.andExpect(jsonPath("$.producer.email").value(ANA_EMAIL));
	}

	@Test
	void AC_AUTH_03_loginWithWrongPasswordReturnsInvalidCredentials() throws Exception {
		expectInvalidCredentials(login(ANA_EMAIL, "Incorrecta2026!"));
	}

	@Test
	void AC_AUTH_03_loginWithUnknownEmailReturnsInvalidCredentials() throws Exception {
		expectInvalidCredentials(login("nadie@aseguradora.test", PASSWORD));
	}

	@Test
	void AC_AUTH_04_loginOfInactiveProducerReturnsInvalidCredentials() throws Exception {
		jdbcTemplate.update("UPDATE producers SET active = FALSE WHERE id = 2");
		try {
			expectInvalidCredentials(login("diego.sosa@aseguradora.test", PASSWORD));
		}
		finally {
			jdbcTemplate.update("UPDATE producers SET active = TRUE WHERE id = 2");
		}
	}

	@Test
	void AC_AUTH_05_quotesWithoutTokenReturnsUnauthorized() throws Exception {
		expectUnauthorized(mockMvc.perform(get("/api/quotes")));
	}

	@Test
	void meWithValidTokenReturnsProducerOfToken() throws Exception {
		String body = login("diego.sosa@aseguradora.test", PASSWORD)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String token = JsonPath.read(body, "$.token");

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(2))
				.andExpect(jsonPath("$.firstName").value("Diego"))
				.andExpect(jsonPath("$.lastName").value("Sosa"))
				.andExpect(jsonPath("$.email").value("diego.sosa@aseguradora.test"));
	}

	@Test
	void meWithoutTokenReturnsUnauthorized() throws Exception {
		expectUnauthorized(mockMvc.perform(get("/api/auth/me")));
	}

	@Test
	void meWithMalformedTokenReturnsUnauthorized() throws Exception {
		expectUnauthorized(mockMvc.perform(get("/api/auth/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-token")));
	}

	@Test
	void meWithExpiredTokenReturnsUnauthorized() throws Exception {
		// Expired one hour before the fixed clock (2026-11-19T10:00:00-03:00), beyond the default clock skew.
		Instant expiresAt = FixedClockConfiguration.NOW.toInstant().minusSeconds(3600);
		String token = jwtEncoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(),
				JwtClaimsSet.builder()
						.subject("1")
						.issuedAt(expiresAt.minusSeconds(8 * 3600))
						.expiresAt(expiresAt)
						.build()))
				.getTokenValue();

		expectUnauthorized(mockMvc.perform(get("/api/auth/me")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)));
	}

	@Test
	void loginWithMissingFieldsReturnsValidationError() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.detail").value("Revisá los datos ingresados"))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field == 'email')].message").value("Completá este campo"))
				.andExpect(jsonPath("$.errors[?(@.field == 'password')].message").value("Completá este campo"));
	}

	private ResultActions login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
	}

	private void expectInvalidCredentials(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.detail").value(INVALID_CREDENTIALS_DETAIL))
				.andExpect(jsonPath("$.errors").isEmpty())
				.andExpect(jsonPath("$.token").doesNotExist());
	}

	private void expectUnauthorized(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value(UNAUTHORIZED_DETAIL))
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors").isEmpty());
	}

}
