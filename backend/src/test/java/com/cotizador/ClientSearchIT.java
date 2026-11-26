package com.cotizador;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Stage 4: client search (AC-CLI-01 to AC-CLI-07; AC-CLI-06 only for the search). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, ClientSearchIT.MutableClockConfiguration.class })
class ClientSearchIT {

	/** Default test clock (backend.md §11), movable for AC-CLI-07. */
	static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-19T10:00:00-03:00");

	private static final String UNDERAGE_DNI = "45111222";

	@TestConfiguration(proxyBeanMethods = false)
	static class MutableClockConfiguration {

		@Bean
		@Primary
		MutableClock mutableClock() {
			return new MutableClock(NOW.toInstant(), ZoneId.of("America/Argentina/Buenos_Aires"));
		}

	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MutableClock clock;

	@AfterEach
	void resetClockAndData() {
		clock.setTo(NOW);
		jdbcTemplate.update("DELETE FROM clients WHERE document_type = 'DNI' AND document_number = ?", UNDERAGE_DNI);
	}

	@Test
	void AC_CLI_01_searchDniReturnsPersonWithAge() throws Exception {
		search("DNI", "30123456")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.documentType").value("DNI"))
				.andExpect(jsonPath("$.documentNumber").value("30123456"))
				.andExpect(jsonPath("$.firstName").value("Lucía"))
				.andExpect(jsonPath("$.lastName").value("Fernández"))
				.andExpect(jsonPath("$.businessName").value((Object) null))
				.andExpect(jsonPath("$.birthDate").value("2004-03-15"))
				.andExpect(jsonPath("$.age").value(22))
				.andExpect(jsonPath("$.email").value("lucia.fernandez@mail.test"))
				.andExpect(jsonPath("$.phone").value("11 5555-0101"))
				.andExpect(jsonPath("$.address").value("Av. Rivadavia 5000"))
				.andExpect(jsonPath("$.postalCode").value("C1406"))
				.andExpect(jsonPath("$.city").value("CABA"))
				.andExpect(jsonPath("$.province").value("CABA"));
	}

	@Test
	void AC_CLI_02_searchCuitWithDashesReturnsCompany() throws Exception {
		search("CUIT", "30-71234567-1")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.documentType").value("CUIT"))
				.andExpect(jsonPath("$.documentNumber").value("30712345671"))
				.andExpect(jsonPath("$.businessName").value("Transportes del Litoral S.A."))
				.andExpect(jsonPath("$.firstName").value((Object) null))
				.andExpect(jsonPath("$.lastName").value((Object) null))
				.andExpect(jsonPath("$.birthDate").value((Object) null))
				.andExpect(jsonPath("$.age").value((Object) null))
				.andExpect(jsonPath("$.city").value("Rosario"))
				.andExpect(jsonPath("$.province").value("Santa Fe"));
	}

	@Test
	void AC_CLI_02_searchCuitWithoutDashesReturnsCompany() throws Exception {
		search("CUIT", "30712345671")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(5))
				.andExpect(jsonPath("$.businessName").value("Transportes del Litoral S.A."));
	}

	@Test
	void AC_CLI_03_searchUnknownDniReturnsNotFound() throws Exception {
		expectError(search("DNI", "99999999"), 404, "CLIENT_NOT_FOUND", "Cliente no encontrado");
	}

	@Test
	void AC_CLI_04_searchDniWithThreeDigitsReturnsInvalidDocument() throws Exception {
		expectError(search("DNI", "123"), 400, "INVALID_DOCUMENT", "El número de documento no es válido");
	}

	@Test
	void AC_CLI_05_searchCuitWithWrongCheckDigitReturnsInvalidDocument() throws Exception {
		expectError(search("CUIT", "30712345672"), 400, "INVALID_DOCUMENT", "El número de documento no es válido");
	}

	@Test
	void AC_CLI_06_searchUnderageClientReturnsUnprocessable() throws Exception {
		insertClientBornOn2008_11_20();

		expectError(search("DNI", UNDERAGE_DNI), 422, "CLIENT_UNDERAGE", "El cliente debe ser mayor de 18 años");
	}

	@Test
	void AC_CLI_07_searchClientTurningEighteenTodayReturnsAge18() throws Exception {
		insertClientBornOn2008_11_20();
		clock.setTo(OffsetDateTime.parse("2026-11-20T10:00:00-03:00"));

		search("DNI", UNDERAGE_DNI)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.documentNumber").value(UNDERAGE_DNI))
				.andExpect(jsonPath("$.birthDate").value("2008-11-20"))
				.andExpect(jsonPath("$.age").value(18));
	}

	@Test
	void searchWithoutTokenReturnsUnauthorized() throws Exception {
		expectError(mockMvc.perform(get("/api/clients/search")
				.param("documentType", "DNI")
				.param("documentNumber", "30123456")), 401, "UNAUTHORIZED", "La sesión expiró. Ingresá nuevamente");
	}

	@Test
	void searchWithoutDocumentNumberReturnsValidationError() throws Exception {
		mockMvc.perform(get("/api/clients/search")
				.param("documentType", "DNI")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAsAna()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("documentNumber"))
				.andExpect(jsonPath("$.errors[0].message").value("Completá este campo"));
	}

	private void insertClientBornOn2008_11_20() {
		jdbcTemplate.update("""
				INSERT INTO clients (document_type, document_number, first_name, last_name, birth_date,
				                     email, phone, address, postal_code, city_id)
				VALUES ('DNI', ?, 'Tomás', 'Benítez', DATE '2008-11-20',
				        'tomas.benitez@mail.test', '11 5555-0909', 'Corrientes 1234', 'C1043', 1)
				""", UNDERAGE_DNI);
	}

	private ResultActions search(String documentType, String documentNumber) throws Exception {
		return mockMvc.perform(get("/api/clients/search")
				.param("documentType", documentType)
				.param("documentNumber", documentNumber)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + loginAsAna()));
	}

	/** Logs in with the current clock, so the token is valid even after moving the clock. */
	private String loginAsAna() throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana.torres@aseguradora.test\",\"password\":\"Demo2026!\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private void expectError(ResultActions result, int status, String code, String detail) throws Exception {
		result.andExpect(status().is(status))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.type").value("about:blank"))
				.andExpect(jsonPath("$.status").value(status))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.detail").value(detail))
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors").isEmpty());
	}

}
