package com.cotizador;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/** Stage 8: insured amount and valuations (AC-VAL-01 to 03, AC-VAL-06), with the fixed clock 2026-11-19. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class InsuredValueIT {

	private static final String VERSION_WITHOUT_VALUATION = "Sin valuación (test)";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private String token;

	@BeforeEach
	void login() throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"ana.torres@aseguradora.test\",\"password\":\"Demo2026!\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		token = JsonPath.read(body, "$.token");
	}

	@AfterEach
	void deleteTestVersion() {
		jdbcTemplate.update("DELETE FROM versions WHERE name = ?", VERSION_WITHOUT_VALUATION);
	}

	/** AC-VAL-01: Gol Base (18.000.000), 8 years old → 60 %. */
	@Test
	void AC_VAL_01_S2_S3_gol2018ReturnsSuggestedAndRange() throws Exception {
		insuredValues("5", "2018")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumAmount").value(8640000.00))
				.andExpect(jsonPath("$.suggestedAmount").value(10800000.00))
				.andExpect(jsonPath("$.maximumAmount").value(12960000.00))
				.andExpect(jsonPath("$.currency").value("ARS"))
				.andExpect(content().string(containsString("\"minimumAmount\":8640000.00")))
				.andExpect(content().string(containsString("\"suggestedAmount\":10800000.00")))
				.andExpect(content().string(containsString("\"maximumAmount\":12960000.00")));
	}

	/** AC-VAL-02: version 41 (110.000.000), 12 years old → 40 %. */
	@Test
	void AC_VAL_02_S2_S3_version41Year2014ReturnsSuggestedAndRange() throws Exception {
		insuredValues("41", "2014")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumAmount").value(35200000.00))
				.andExpect(jsonPath("$.suggestedAmount").value(44000000.00))
				.andExpect(jsonPath("$.maximumAmount").value(52800000.00))
				.andExpect(jsonPath("$.currency").value("ARS"));
	}

	/** AC-VAL-03: version 29 (52.000.000), 16 years old → 30 % floor instead of 20 %. */
	@Test
	void AC_VAL_03_S2_version29Year2010StopsAtThirtyPercentFloor() throws Exception {
		insuredValues("29", "2010")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.minimumAmount").value(12480000.00))
				.andExpect(jsonPath("$.suggestedAmount").value(15600000.00))
				.andExpect(jsonPath("$.maximumAmount").value(18720000.00))
				.andExpect(jsonPath("$.currency").value("ARS"));
	}

	/** AC-VAL-06: a version with reference_value NULL, inserted only for this test. */
	@Test
	void AC_VAL_06_S6_versionWithoutReferenceValueReturnsValuationNotFound() throws Exception {
		Long versionId = jdbcTemplate.queryForObject(
				"INSERT INTO versions (model_id, name, reference_value) VALUES (3, ?, NULL) RETURNING id",
				Long.class, VERSION_WITHOUT_VALUATION);

		insuredValues(versionId.toString(), "2018")
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALUATION_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("No hay valuación disponible para este vehículo"))
				.andExpect(jsonPath("$.errors", hasSize(0)));
	}

	@Test
	void S6_unknownVersionReturnsValuationNotFound() throws Exception {
		insuredValues("999999", "2018")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("VALUATION_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("No hay valuación disponible para este vehículo"));
	}

	@Test
	void V3_yearBeforeMinimumReturnsInvalidYear() throws Exception {
		insuredValues("5", "2009")
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_YEAR"))
				.andExpect(jsonPath("$.detail").value("El año debe estar entre 2010 y 2026"))
				.andExpect(jsonPath("$.errors", hasSize(0)));
	}

	@Test
	void V3_yearAfterCurrentYearReturnsInvalidYear() throws Exception {
		insuredValues("5", "2027")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_YEAR"))
				.andExpect(jsonPath("$.detail").value("El año debe estar entre 2010 y 2026"));
	}

	@Test
	void V3_invalidYearIsReportedBeforeMissingValuation() throws Exception {
		insuredValues("999999", "2009")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_YEAR"));
	}

	@Test
	void shouldRejectMissingYearWithValidationError() throws Exception {
		perform(get("/api/insured-values").param("versionId", "5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("year"))
				.andExpect(jsonPath("$.errors[0].message").value("Completá este campo"));
	}

	@Test
	void shouldRejectNonNumericVersionIdWithValidationError() throws Exception {
		insuredValues("abc", "2018")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("versionId"));
	}

	@Test
	void shouldRejectRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/insured-values").param("versionId", "5").param("year", "2018"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private ResultActions insuredValues(String versionId, String year) throws Exception {
		return perform(get("/api/insured-values").param("versionId", versionId).param("year", year));
	}

	private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

}
