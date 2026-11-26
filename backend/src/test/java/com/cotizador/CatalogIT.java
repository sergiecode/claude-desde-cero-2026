package com.cotizador;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.jayway.jsonpath.JsonPath;

/** Stage 5: vehicle catalogs (AC-VEH-01 to AC-VEH-05), with the fixed clock 2026-11-19. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class CatalogIT {

	@Autowired
	private MockMvc mockMvc;

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

	/** AC-VEH-01: 17 years, from 2026 down to 2010. */
	@Test
	void AC_VEH_01_shouldListMotoYearsDescending_V3() throws Exception {
		perform(get("/api/catalogs/years").param("vehicleType", "MOTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(17)))
				.andExpect(jsonPath("$", contains(2026, 2025, 2024, 2023, 2022, 2021, 2020, 2019, 2018, 2017,
						2016, 2015, 2014, 2013, 2012, 2011, 2010)));
	}

	/** AC-VEH-02: only Gol, because Polo exists from 2018. */
	@Test
	void AC_VEH_02_shouldListOnlyGolForVolkswagenAuto2015_V4() throws Exception {
		perform(get("/api/catalogs/models")
				.param("vehicleType", "AUTO")
				.param("year", "2015")
				.param("brandId", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(3))
				.andExpect(jsonPath("$[0].name").value("Gol"));
	}

	/** AC-VEH-03: only Ranger, because Maverick exists from 2022. */
	@Test
	void AC_VEH_03_shouldListOnlyRangerForFordCamioneta2020_V4() throws Exception {
		perform(get("/api/catalogs/models")
				.param("vehicleType", "CAMIONETA")
				.param("year", "2020")
				.param("brandId", "8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(15))
				.andExpect(jsonPath("$[0].name").value("Ranger"));
	}

	/** AC-VEH-04: Fiat, Toyota, Volkswagen ordered by name (Fiat appears because of Argo). */
	@Test
	void AC_VEH_04_shouldListAutoBrandsFor2015OrderedByName_V4() throws Exception {
		perform(get("/api/catalogs/brands")
				.param("vehicleType", "AUTO")
				.param("year", "2015"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[*].id", contains(3, 1, 2)))
				.andExpect(jsonPath("$[*].name", contains("Fiat", "Toyota", "Volkswagen")));
	}

	/** AC-VEH-05: PARTICULAR, COMERCIAL, DELIVERY in enum order. */
	@Test
	void AC_VEH_05_shouldListMotoUsageTypesInEnumOrder_V5() throws Exception {
		perform(get("/api/catalogs/usage-types").param("vehicleType", "MOTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", contains("PARTICULAR", "COMERCIAL", "DELIVERY")));
	}

	@Test
	void shouldListVehicleTypesInEnumOrder() throws Exception {
		perform(get("/api/catalogs/vehicle-types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", contains("AUTO", "MOTO", "CAMIONETA", "CAMION")));
	}

	@Test
	void shouldListAutoUsageTypesInEnumOrder_V5() throws Exception {
		perform(get("/api/catalogs/usage-types").param("vehicleType", "AUTO"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", contains("PARTICULAR", "COMERCIAL", "TAXI_REMIS", "DELIVERY")));
	}

	@Test
	void shouldListVersionsOfModelOrderedById_V4() throws Exception {
		perform(get("/api/catalogs/versions").param("modelId", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", contains(5, 6)))
				.andExpect(jsonPath("$[*].name", contains("Base", "Full")));
	}

	@Test
	void shouldListModelsOrderedByName_V4() throws Exception {
		perform(get("/api/catalogs/models")
				.param("vehicleType", "AUTO")
				.param("year", "2020")
				.param("brandId", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].name", contains("Argo", "Cronos")));
	}

	@Test
	void shouldReturnEmptyModelsWhenBrandIsOfAnotherType_V4() throws Exception {
		perform(get("/api/catalogs/models")
				.param("vehicleType", "AUTO")
				.param("year", "2020")
				.param("brandId", "8"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void shouldReturnEmptyVersionsForUnknownModel() throws Exception {
		perform(get("/api/catalogs/versions").param("modelId", "999"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void shouldRejectMissingYearWithValidationError() throws Exception {
		perform(get("/api/catalogs/brands").param("vehicleType", "AUTO"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.detail").value("Revisá los datos ingresados"))
				.andExpect(jsonPath("$.errors[0].field").value("year"))
				.andExpect(jsonPath("$.errors[0].message").value("Completá este campo"));
	}

	@Test
	void shouldRejectUnknownVehicleTypeWithValidationError() throws Exception {
		perform(get("/api/catalogs/years").param("vehicleType", "BICI"))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("vehicleType"));
	}

	@Test
	void shouldRejectNonNumericBrandIdWithValidationError() throws Exception {
		perform(get("/api/catalogs/models")
				.param("vehicleType", "AUTO")
				.param("year", "2020")
				.param("brandId", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("brandId"));
	}

	@Test
	void shouldRejectRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/catalogs/years").param("vehicleType", "AUTO"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("La sesión expiró. Ingresá nuevamente"));
	}

	private ResultActions perform(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}

}
