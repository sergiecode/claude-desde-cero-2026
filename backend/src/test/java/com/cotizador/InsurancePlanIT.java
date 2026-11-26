package com.cotizador;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

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

import com.jayway.jsonpath.JsonPath;

/** Stage 7: insurance plans (AC-PLAN-01), rules P1 and P2. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class InsurancePlanIT {

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

	/** AC-PLAN-01: 3 plans in display order, with id, code, name and description, and without the rate. */
	@Test
	void AC_PLAN_01_shouldListThreePlansInDisplayOrderWithoutRate_P1_P2() throws Exception {
		mockMvc.perform(get("/api/insurance-plans").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[*].id", contains(1, 2, 3)))
				.andExpect(jsonPath("$[*].code", contains("RC", "TERCEROS_COMPLETO", "TODO_RIESGO")))
				.andExpect(jsonPath("$[*].name",
						contains("RC — Responsabilidad civil", "Terceros completo", "Todo riesgo")))
				.andExpect(jsonPath("$[*].description", contains(
						"Daños a terceros",
						"RC + robo e incendio totales y parciales + destrucción total + granizo + cristales",
						"Terceros completo + daños parciales al propio vehículo")))
				.andExpect(jsonPath("$[0]", hasExactlyContractKeys()))
				.andExpect(jsonPath("$[1]", hasExactlyContractKeys()))
				.andExpect(jsonPath("$[2]", hasExactlyContractKeys()))
				.andExpect(content().string(not(containsStringIgnoringCase("rate"))))
				.andExpect(content().string(not(containsStringIgnoringCase("tasa"))));
	}

	@Test
	void shouldRejectRequestWithoutToken() throws Exception {
		mockMvc.perform(get("/api/insurance-plans"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.detail").value("La sesión expiró. Ingresá nuevamente"));
	}

	/** Matches a JSON object whose keys are exactly id, code, name and description. */
	private static org.hamcrest.Matcher<Map<String, Object>> hasExactlyContractKeys() {
		return new org.hamcrest.TypeSafeMatcher<>() {
			@Override
			protected boolean matchesSafely(Map<String, Object> plan) {
				return containsInAnyOrder("id", "code", "name", "description").matches(plan.keySet());
			}

			@Override
			public void describeTo(org.hamcrest.Description description) {
				description.appendText("an object with exactly the keys id, code, name and description");
			}
		};
	}

}
