package com.cotizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.cotizador.repository.QuoteRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Stage 9: tariffs and price calculation ({@code POST /api/quotes/calculate}), with the fixed clock 2026-11-19 and
 * Ana Torres logged in.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class QuoteCalculateIT {

	private static final String UNDERAGE_DNI = "50111222";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private QuoteRepository quoteRepository;

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
	void deleteTestClient() {
		jdbcTemplate.update("DELETE FROM clients WHERE document_type = 'DNI' AND document_number = ?", UNDERAGE_DNI);
	}

	// --- Price (manual §7) ---

	@Test
	void AC_PRICE_01_autoInCabaYoungDriver() throws Exception {
		calculate(example1())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insuredAmount").value(10800000.00))
				.andExpect(jsonPath("$.planRate").value(0.0025))
				.andExpect(jsonPath("$.basePremium").value(27000.00))
				.andExpect(jsonPath("$.zone.code").value("ZONA_1"))
				.andExpect(jsonPath("$.zone.name").value("CABA"))
				.andExpect(jsonPath("$.zone.factor").value(1.20))
				.andExpect(jsonPath("$.usageFactor").value(1.00))
				.andExpect(jsonPath("$.vehicleAgeYears").value(8))
				.andExpect(jsonPath("$.vehicleAgeFactor").value(1.10))
				.andExpect(jsonPath("$.driverAge").value(22))
				.andExpect(jsonPath("$.driverAgeFactor").value(1.30))
				.andExpect(jsonPath("$.premium").value(46332.00))
				.andExpect(jsonPath("$.taxRate").value(0.21))
				.andExpect(jsonPath("$.taxes").value(9729.72))
				.andExpect(jsonPath("$.totalPrice").value(56061.72))
				.andExpect(jsonPath("$.currency").value("ARS"))
				// Money with 2 decimals and factors as in api.md §8.2
				.andExpect(content().string(containsString("\"insuredAmount\":10800000.00")))
				.andExpect(content().string(containsString("\"basePremium\":27000.00")))
				.andExpect(content().string(containsString("\"premium\":46332.00")))
				.andExpect(content().string(containsString("\"taxes\":9729.72")))
				.andExpect(content().string(containsString("\"totalPrice\":56061.72")))
				.andExpect(content().string(containsString("\"zone\":{\"code\":\"ZONA_1\",\"name\":\"CABA\",\"factor\":1.20}")))
				.andExpect(content().string(containsString("\"planRate\":0.0025")));
	}

	@Test
	void AC_PRICE_02_companyTruckInRosario() throws Exception {
		Map<String, Object> request = request(5, "CAMION", 2014, 11, 21, 41, "TRANSPORTE_CARGA", "ABC123", 1,
				"50000000.00");

		calculate(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.basePremium").value(50000.00))
				.andExpect(jsonPath("$.zone.code").value("ZONA_5"))
				.andExpect(jsonPath("$.zone.factor").value(1.10))
				.andExpect(jsonPath("$.usageFactor").value(1.25))
				.andExpect(jsonPath("$.vehicleAgeYears").value(12))
				.andExpect(jsonPath("$.vehicleAgeFactor").value(1.20))
				.andExpect(content().string(containsString("\"driverAge\":null")))
				.andExpect(jsonPath("$.driverAgeFactor").value(1.00))
				.andExpect(jsonPath("$.premium").value(82500.00))
				.andExpect(jsonPath("$.taxes").value(17325.00))
				.andExpect(jsonPath("$.totalPrice").value(99825.00));
	}

	@Test
	void AC_PRICE_03_motoDeliveryRoundsOnlyAtTheEnd() throws Exception {
		calculate(example3())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.zone.code").value("ZONA_2"))
				.andExpect(jsonPath("$.zone.factor").value(1.15))
				.andExpect(jsonPath("$.usageFactor").value(1.30))
				.andExpect(jsonPath("$.vehicleAgeYears").value(2))
				.andExpect(jsonPath("$.vehicleAgeFactor").value(1.00))
				.andExpect(jsonPath("$.driverAge").value(27))
				.andExpect(jsonPath("$.driverAgeFactor").value(1.15))
				.andExpect(jsonPath("$.premium").value(17407.41))
				.andExpect(jsonPath("$.taxes").value(3655.56))
				.andExpect(jsonPath("$.totalPrice").value(21062.97));
	}

	@Test
	void AC_PRICE_04_maximumAgeAndVehicleAgeFactors() throws Exception {
		Map<String, Object> request = request(4, "CAMIONETA", 2010, 8, 15, 29, "AGRICOLA", "ABC123", 2,
				"15600000.00");

		calculate(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.zone.code").value("ZONA_7"))
				.andExpect(jsonPath("$.zone.factor").value(0.95))
				.andExpect(jsonPath("$.usageFactor").value(1.05))
				.andExpect(jsonPath("$.vehicleAgeYears").value(16))
				.andExpect(jsonPath("$.vehicleAgeFactor").value(1.20))
				.andExpect(jsonPath("$.driverAge").value(72))
				.andExpect(jsonPath("$.driverAgeFactor").value(1.15))
				.andExpect(jsonPath("$.premium").value(53685.45))
				.andExpect(jsonPath("$.taxes").value(11273.94))
				.andExpect(jsonPath("$.totalPrice").value(64959.39));
	}

	@Test
	void AC_PRICE_05_Q1_calculateTwiceGivesTheSameResultAndSavesNothing() throws Exception {
		long quotesBefore = quoteRepository.count();

		String first = calculate(example1()).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		String second = calculate(example1()).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();

		assertThat(second).isEqualTo(first);
		assertThat(quotesBefore).isZero();
		assertThat(quoteRepository.count()).isZero();
	}

	// --- Insured amount (S5) ---

	@Test
	void AC_VAL_04_S5_insuredAmountBelowMinimumIsOutOfRange() throws Exception {
		Map<String, Object> request = example1();
		request.put("insuredAmount", "8639000.00");

		expectError(calculate(request), 400, "INSURED_AMOUNT_OUT_OF_RANGE",
				"La suma asegurada debe estar entre $ 8.640.000,00 y $ 12.960.000,00");
	}

	@Test
	void AC_VAL_05_S5_insuredAmountEqualToMaximumIsAccepted() throws Exception {
		Map<String, Object> request = example1();
		request.put("insuredAmount", "12960000.00");

		calculate(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.insuredAmount").value(12960000.00));
	}

	@Test
	void S5_insuredAmountAboveMaximumIsOutOfRange() throws Exception {
		Map<String, Object> request = example1();
		request.put("insuredAmount", "12960000.01");

		expectError(calculate(request), 400, "INSURED_AMOUNT_OUT_OF_RANGE",
				"La suma asegurada debe estar entre $ 8.640.000,00 y $ 12.960.000,00");
	}

	// --- Vehicle (V3 to V6) ---

	@Test
	void AC_VEH_06_V3_year2009IsInvalid() throws Exception {
		Map<String, Object> request = example1();
		request.put("manufacturingYear", 2009);

		expectError(calculate(request), 400, "INVALID_YEAR", "El año debe estar entre 2010 y 2026");
	}

	@Test
	void AC_VEH_07_V5_motoWithTaxiUsageIsNotAllowed() throws Exception {
		Map<String, Object> request = example3();
		request.put("usageType", "TAXI_REMIS");

		expectError(calculate(request), 400, "USAGE_NOT_ALLOWED",
				"El tipo de uso no está permitido para este vehículo");
	}

	@Test
	void AC_VEH_08_V4_versionOfAnotherModelIsInvalid() throws Exception {
		Map<String, Object> request = example1();
		request.put("versionId", 13);

		expectError(calculate(request), 400, "INVALID_VEHICLE",
				"La marca, el modelo o la versión no corresponden al tipo y año elegidos");
	}

	@Test
	void AC_VEH_09_V4_modelNotAvailableForTheYearIsInvalid() throws Exception {
		Map<String, Object> request = example1();
		request.put("modelId", 4);
		request.put("versionId", 7);
		request.put("manufacturingYear", 2015);

		expectError(calculate(request), 400, "INVALID_VEHICLE",
				"La marca, el modelo o la versión no corresponden al tipo y año elegidos");
	}

	@Test
	void V4_brandOfAnotherVehicleTypeIsInvalid() throws Exception {
		Map<String, Object> request = example1();
		request.put("vehicleType", "MOTO");

		expectError(calculate(request), 400, "INVALID_VEHICLE",
				"La marca, el modelo o la versión no corresponden al tipo y año elegidos");
	}

	@Test
	void AC_VEH_10_V6_patentWithWrongFormatIsInvalid() throws Exception {
		Map<String, Object> request = example1();
		request.put("patent", "AB123CD");

		expectError(calculate(request), 400, "INVALID_PATENT", "La patente debe tener el formato ABC123")
				.andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors[0].field").value("patent"))
				.andExpect(jsonPath("$.errors[0].message").value("La patente debe tener el formato ABC123"));
	}

	@Test
	void AC_VEH_11_V6_calculateAcceptsPatentWithSpacesAndLowercase() throws Exception {
		Map<String, Object> request = example1();
		request.put("patent", " abc123 ");

		calculate(request)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.premium").value(46332.00));
	}

	// --- Client (C1, C5) ---

	@Test
	void AC_CLI_06_C5_calculateForUnderageClientReturnsUnprocessable() throws Exception {
		Long clientId = jdbcTemplate.queryForObject("""
				INSERT INTO clients (document_type, document_number, first_name, last_name, birth_date,
				                     email, phone, address, postal_code, city_id)
				VALUES ('DNI', ?, 'Tomás', 'Benítez', DATE '2008-11-20',
				        'tomas.benitez@mail.test', '11 5555-0909', 'Corrientes 1234', 'C1043', 1)
				RETURNING id
				""", Long.class, UNDERAGE_DNI);
		Map<String, Object> request = example1();
		request.put("clientId", clientId);

		expectError(calculate(request), 422, "CLIENT_UNDERAGE", "El cliente debe ser mayor de 18 años");
	}

	@Test
	void C1_unknownClientReturnsNotFound() throws Exception {
		Map<String, Object> request = example1();
		request.put("clientId", 999999);

		expectError(calculate(request), 404, "CLIENT_NOT_FOUND", "Cliente no encontrado");
	}

	// --- Other errors of api.md §8.2 ---

	@Test
	void P1_unknownPlanReturnsPlanNotFound() throws Exception {
		Map<String, Object> request = example1();
		request.put("planId", 999);

		expectError(calculate(request), 400, "PLAN_NOT_FOUND", "El plan seleccionado no existe");
	}

	@Test
	void validationsFollowTheOrderOfBackendSpec() throws Exception {
		// Invalid year (step 3) and invalid patent (step 6): the year is reported.
		Map<String, Object> request = example1();
		request.put("manufacturingYear", 2009);
		request.put("patent", "AB123CD");
		expectError(calculate(request), 400, "INVALID_YEAR", "El año debe estar entre 2010 y 2026");

		// Invalid patent (step 6) and unknown plan (step 7): the patent is reported.
		request = example1();
		request.put("patent", "AB123CD");
		request.put("planId", 999);
		expectError(calculate(request), 400, "INVALID_PATENT", "La patente debe tener el formato ABC123");

		// Not allowed usage (step 5) and out of range amount (step 8): the usage is reported.
		request = example3();
		request.put("usageType", "TAXI_REMIS");
		request.put("insuredAmount", "1.00");
		expectError(calculate(request), 400, "USAGE_NOT_ALLOWED",
				"El tipo de uso no está permitido para este vehículo");
	}

	@Test
	void missingFieldsReturnValidationErrorPerField() throws Exception {
		mockMvc.perform(post("/api/quotes/calculate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.detail").value("Revisá los datos ingresados"))
				.andExpect(jsonPath("$.errors", hasSize(10)))
				.andExpect(jsonPath("$.errors[?(@.field == 'patent')].message").value("Completá este campo"))
				.andExpect(jsonPath("$.errors[?(@.field == 'insuredAmount')].message").value("Completá este campo"));
	}

	@Test
	void insuredAmountWithMoreThanTwoDecimalsIsAValidationError() throws Exception {
		Map<String, Object> request = example1();
		request.put("insuredAmount", "10800000.001");

		calculate(request)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("insuredAmount"));
	}

	@Test
	void zeroInsuredAmountIsAValidationError() throws Exception {
		Map<String, Object> request = example1();
		request.put("insuredAmount", "0");

		calculate(request)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("insuredAmount"));
	}

	@Test
	void calculateWithoutTokenIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/quotes/calculate")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(example1())))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	// --- Helpers ---

	/** AC-PRICE-01: Lucía Fernández, VW Gol Base 2018, Particular, Terceros completo, 10.800.000. */
	private static Map<String, Object> example1() {
		return request(1, "AUTO", 2018, 2, 3, 5, "PARTICULAR", "abc123", 2, "10800000.00");
	}

	/** AC-PRICE-03: Martín Gómez, Honda Wave 110 Base 2024, Delivery, Todo riesgo, 2.250.000. */
	private static Map<String, Object> example3() {
		return request(2, "MOTO", 2024, 4, 7, 13, "DELIVERY", "ABC123", 3, "2250000.00");
	}

	private static Map<String, Object> request(long clientId, String vehicleType, int year, long brandId,
			long modelId, long versionId, String usageType, String patent, long planId, String insuredAmount) {
		Map<String, Object> request = new LinkedHashMap<>();
		request.put("clientId", clientId);
		request.put("vehicleType", vehicleType);
		request.put("manufacturingYear", year);
		request.put("brandId", brandId);
		request.put("modelId", modelId);
		request.put("versionId", versionId);
		request.put("usageType", usageType);
		request.put("patent", patent);
		request.put("planId", planId);
		request.put("insuredAmount", insuredAmount);
		return request;
	}

	/** Writes the request as JSON; {@code insuredAmount} goes as a JSON number, the rest as given. */
	private static String json(Map<String, Object> request) {
		return request.entrySet().stream()
				.map(entry -> "\"" + entry.getKey() + "\":" + jsonValue(entry.getKey(), entry.getValue()))
				.collect(Collectors.joining(",", "{", "}"));
	}

	private static String jsonValue(String key, Object value) {
		if (value instanceof String text && !key.equals("insuredAmount")) {
			return "\"" + text + "\"";
		}
		return String.valueOf(value);
	}

	private ResultActions calculate(Map<String, Object> request) throws Exception {
		return mockMvc.perform(post("/api/quotes/calculate")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(json(request)));
	}

	private static ResultActions expectError(ResultActions result, int status, String code, String detail)
			throws Exception {
		return result
				.andExpect(status().is(status))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value(code))
				.andExpect(jsonPath("$.detail").value(detail));
	}

}
