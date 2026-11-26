package com.cotizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

/**
 * Stage 10: saving and reading quotes (AC-QUO-01 to AC-QUO-09, AC-VEH-11). The clock starts at
 * 2026-11-19T10:00:00-03:00 and is moved only by the criteria that need another date.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, QuoteIT.MutableClockConfiguration.class })
class QuoteIT {

	static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-19T10:00:00-03:00");

	private static final String ANA = "ana.torres@aseguradora.test";
	private static final String DIEGO = "diego.sosa@aseguradora.test";

	/** AC-PRICE-01: Lucía Fernández, VW Gol Base 2018, Particular, Terceros completo, 10.800.000. */
	private static final String EXAMPLE_1 = """
			{"clientId":1,"vehicleType":"AUTO","manufacturingYear":2018,"brandId":2,"modelId":3,"versionId":5,
			 "usageType":"PARTICULAR","patent":"abc123","planId":2,"insuredAmount":10800000.00}""";

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

	private BigDecimal zone1Factor;

	@BeforeEach
	void cleanQuotes() {
		clock.setTo(NOW);
		jdbcTemplate.update("DELETE FROM policies");
		jdbcTemplate.update("DELETE FROM quotes");
		jdbcTemplate.update("DELETE FROM number_sequences");
		zone1Factor = jdbcTemplate.queryForObject("SELECT factor FROM tariff_zones WHERE code = 'ZONA_1'",
				BigDecimal.class);
	}

	@AfterEach
	void restore() {
		clock.setTo(NOW);
		jdbcTemplate.update("UPDATE tariff_zones SET factor = ? WHERE code = 'ZONA_1'", zone1Factor);
	}

	@Test
	void AC_QUO_01_saveFirstQuoteOfTheYear() throws Exception {
		String body = save(ANA, EXAMPLE_1)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"))
				.andExpect(jsonPath("$.status").value("CALCULATED"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-19T10:00:00-03:00"))
				.andExpect(jsonPath("$.validUntil").value("2026-12-04T10:00:00-03:00"))
				.andExpect(jsonPath("$.calculation.insuredAmount").value(10800000.00))
				.andExpect(jsonPath("$.calculation.planRate").value(0.0025))
				.andExpect(jsonPath("$.calculation.basePremium").value(27000.00))
				.andExpect(jsonPath("$.calculation.zone.code").value("ZONA_1"))
				.andExpect(jsonPath("$.calculation.zone.name").value("CABA"))
				.andExpect(jsonPath("$.calculation.zone.factor").value(1.20))
				.andExpect(jsonPath("$.calculation.usageFactor").value(1.00))
				.andExpect(jsonPath("$.calculation.vehicleAgeYears").value(8))
				.andExpect(jsonPath("$.calculation.vehicleAgeFactor").value(1.10))
				.andExpect(jsonPath("$.calculation.driverAge").value(22))
				.andExpect(jsonPath("$.calculation.driverAgeFactor").value(1.30))
				.andExpect(jsonPath("$.calculation.premium").value(46332.00))
				.andExpect(jsonPath("$.calculation.taxRate").value(0.21))
				.andExpect(jsonPath("$.calculation.taxes").value(9729.72))
				.andExpect(jsonPath("$.calculation.totalPrice").value(56061.72))
				.andExpect(jsonPath("$.calculation.currency").value("ARS"))
				.andExpect(content().string(containsString("\"totalPrice\":56061.72")))
				.andExpect(content().string(containsString("\"policy\":null")))
				.andReturn().getResponse().getContentAsString();

		Integer id = JsonPath.read(body, "$.id");
		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM quotes WHERE id = ?", id);
		assertThat(row.get("quote_number")).isEqualTo("COT-2026-000001");
		assertThat(row.get("status")).isEqualTo("CALCULATED");
		assertThat(row.get("producer_id")).isEqualTo(1L);
		assertThat(row.get("brand_name")).isEqualTo("Volkswagen");
		assertThat(row.get("model_name")).isEqualTo("Gol");
		assertThat(row.get("version_name")).isEqualTo("Base");
		assertThat(row.get("plan_name")).isEqualTo("Terceros completo");
		assertThat((BigDecimal) row.get("plan_rate")).isEqualByComparingTo("0.0025");
		assertThat((BigDecimal) row.get("zone_factor")).isEqualByComparingTo("1.20");
		assertThat(row.get("driver_age")).isEqualTo(22);
		assertThat((BigDecimal) row.get("premium")).isEqualByComparingTo("46332.00");
		assertThat((BigDecimal) row.get("taxes")).isEqualByComparingTo("9729.72");
		assertThat((BigDecimal) row.get("total_price")).isEqualByComparingTo("56061.72");
		assertThat(row.get("currency")).isEqualTo("ARS");
	}

	@Test
	void AC_QUO_01_locationHeaderPointsToTheSavedQuote() throws Exception {
		ResultActions result = save(ANA, EXAMPLE_1).andExpect(status().isCreated());
		Integer id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");

		result.andExpect(header().string(HttpHeaders.LOCATION, "/api/quotes/" + id));
		read(ANA, id).andExpect(status().isOk()).andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"));
	}

	@Test
	void AC_QUO_02_secondQuoteGetsTheNextNumber() throws Exception {
		save(ANA, EXAMPLE_1).andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"));
		save(DIEGO, EXAMPLE_1).andExpect(status().isCreated())
				.andExpect(jsonPath("$.quoteNumber").value("COT-2026-000002"));
	}

	@Test
	void AC_QUO_03_numberRestartsInANewYear() throws Exception {
		save(ANA, EXAMPLE_1).andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"));
		save(ANA, EXAMPLE_1).andExpect(jsonPath("$.quoteNumber").value("COT-2026-000002"));

		clock.setTo(OffsetDateTime.parse("2027-01-02T10:00:00-03:00"));

		save(ANA, EXAMPLE_1).andExpect(status().isCreated())
				.andExpect(jsonPath("$.quoteNumber").value("COT-2027-000001"));
	}

	@Test
	void AC_QUO_04_savedQuoteKeepsItsValuesWhenTariffsChange() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		jdbcTemplate.update("UPDATE tariff_zones SET factor = 1.50 WHERE code = 'ZONA_1'");

		read(ANA, id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.calculation.zone.factor").value(1.20))
				.andExpect(jsonPath("$.calculation.premium").value(46332.00))
				.andExpect(jsonPath("$.calculation.taxes").value(9729.72))
				.andExpect(jsonPath("$.calculation.totalPrice").value(56061.72));
	}

	@Test
	void AC_QUO_05_quoteIsStillValidAtItsValidUntil() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		clock.setTo(OffsetDateTime.parse("2026-12-04T10:00:00-03:00"));

		read(ANA, id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CALCULATED"));
		assertThat(statusInDatabase(id)).isEqualTo("CALCULATED");
	}

	@Test
	void AC_QUO_06_quoteExpiresOneSecondAfterItsValidUntilAndIsSaved() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		clock.setTo(OffsetDateTime.parse("2026-12-04T10:00:01-03:00"));

		read(ANA, id).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
		assertThat(statusInDatabase(id)).isEqualTo("EXPIRED");
	}

	@Test
	void AC_QUO_06_listAlsoExpiresOverdueQuotes() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		clock.setTo(OffsetDateTime.parse("2026-12-04T10:00:01-03:00"));

		list(ANA).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("EXPIRED"));
		assertThat(statusInDatabase(id)).isEqualTo("EXPIRED");
	}

	@Test
	void AC_QUO_07_producerSeesOnlyHisQuotesMostRecentFirst() throws Exception {
		Integer older = savedId(ANA, EXAMPLE_1);
		clock.setTo(NOW.plusHours(1));
		Integer diegos = savedId(DIEGO, EXAMPLE_1);
		clock.setTo(NOW.plusHours(2));
		Integer newer = savedId(ANA, EXAMPLE_1);
		clock.setTo(NOW.plusHours(3));

		list(ANA)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].id").value(newer))
				.andExpect(jsonPath("$[0].quoteNumber").value("COT-2026-000003"))
				.andExpect(jsonPath("$[0].createdAt").value("2026-11-19T12:00:00-03:00"))
				.andExpect(jsonPath("$[0].clientName").value("Lucía Fernández"))
				.andExpect(jsonPath("$[0].vehicleType").value("AUTO"))
				.andExpect(jsonPath("$[0].brandName").value("Volkswagen"))
				.andExpect(jsonPath("$[0].modelName").value("Gol"))
				.andExpect(jsonPath("$[0].planName").value("Terceros completo"))
				.andExpect(jsonPath("$[0].totalPrice").value(56061.72))
				.andExpect(jsonPath("$[0].currency").value("ARS"))
				.andExpect(jsonPath("$[0].status").value("CALCULATED"))
				.andExpect(jsonPath("$[1].id").value(older));

		list(DIEGO).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(diegos));
	}

	@Test
	void AC_QUO_07_companyClientNameIsTheBusinessName() throws Exception {
		save(ANA, """
				{"clientId":5,"vehicleType":"CAMION","manufacturingYear":2014,"brandId":11,"modelId":21,
				 "versionId":41,"usageType":"TRANSPORTE_CARGA","patent":"ABC123","planId":1,
				 "insuredAmount":50000000.00}""").andExpect(status().isCreated());

		String expected = jdbcTemplate.queryForObject("SELECT business_name FROM clients WHERE id = 5", String.class);
		list(ANA).andExpect(jsonPath("$[0].clientName").value(expected));
	}

	@Test
	void AC_QUO_08_quoteOfAnotherProducerIsNotFound() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		read(DIEGO, id)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Cotización no encontrada"));
	}

	@Test
	void AC_QUO_08_unknownQuoteIsNotFound() throws Exception {
		read(ANA, 999999).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
	}

	@Test
	void AC_QUO_09_totalPriceSentByTheClientIsIgnored() throws Exception {
		String withTotal = EXAMPLE_1.replace("\"planId\":2", "\"planId\":2,\"totalPrice\":1.00,\"premium\":1.00");

		Integer id = JsonPath.read(save(ANA, withTotal)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.calculation.premium").value(46332.00))
				.andExpect(jsonPath("$.calculation.totalPrice").value(56061.72))
				.andReturn().getResponse().getContentAsString(), "$.id");

		BigDecimal stored = jdbcTemplate.queryForObject("SELECT total_price FROM quotes WHERE id = ?",
				BigDecimal.class, id);
		assertThat(stored).isEqualByComparingTo("56061.72");
	}

	@Test
	void AC_VEH_11_patentWithSpacesAndLowercaseIsSavedUppercase() throws Exception {
		Integer id = JsonPath.read(save(ANA, EXAMPLE_1.replace("\"abc123\"", "\" abc123 \""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.vehicle.patent").value("ABC123"))
				.andReturn().getResponse().getContentAsString(), "$.id");

		assertThat(jdbcTemplate.queryForObject("SELECT patent FROM quotes WHERE id = ?", String.class, id))
				.isEqualTo("ABC123");
		read(ANA, id).andExpect(jsonPath("$.vehicle.patent").value("ABC123"));
	}

	@Test
	void detailHasClientVehicleAndCalculationAsInApiSpec() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		read(ANA, id)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"))
				.andExpect(jsonPath("$.createdAt").value("2026-11-19T10:00:00-03:00"))
				.andExpect(jsonPath("$.validUntil").value("2026-12-04T10:00:00-03:00"))
				.andExpect(jsonPath("$.client.id").value(1))
				.andExpect(jsonPath("$.client.documentType").value("DNI"))
				.andExpect(jsonPath("$.client.firstName").value("Lucía"))
				.andExpect(jsonPath("$.client.lastName").value("Fernández"))
				.andExpect(jsonPath("$.client.age").value(22))
				.andExpect(jsonPath("$.client.city").value("CABA"))
				.andExpect(jsonPath("$.vehicle.vehicleType").value("AUTO"))
				.andExpect(jsonPath("$.vehicle.manufacturingYear").value(2018))
				.andExpect(jsonPath("$.vehicle.brandName").value("Volkswagen"))
				.andExpect(jsonPath("$.vehicle.modelName").value("Gol"))
				.andExpect(jsonPath("$.vehicle.versionName").value("Base"))
				.andExpect(jsonPath("$.vehicle.patent").value("ABC123"))
				.andExpect(jsonPath("$.vehicle.usageType").value("PARTICULAR"))
				.andExpect(jsonPath("$.planName").value("Terceros completo"))
				.andExpect(jsonPath("$.calculation.zone.name").value("CABA"))
				.andExpect(jsonPath("$.calculation.totalPrice").value(56061.72))
				.andExpect(content().string(containsString("\"policy\":null")));
	}

	@Test
	void clientAgeIsTheOneStoredWhenQuoting() throws Exception {
		Integer id = savedId(ANA, EXAMPLE_1);

		// Lucía (born 2004-03-15) turns 23 on 2027-03-15; the quote keeps the age used to quote.
		clock.setTo(OffsetDateTime.parse("2027-03-20T10:00:00-03:00"));

		read(ANA, id).andExpect(jsonPath("$.client.age").value(22));
	}

	@Test
	void saveWithErrorsSavesNothing() throws Exception {
		save(ANA, EXAMPLE_1.replace("\"abc123\"", "\"AB123CD\""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PATENT"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quotes", Integer.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM number_sequences", Integer.class)).isZero();
	}

	@Test
	void quotesRequireAToken() throws Exception {
		mockMvc.perform(get("/api/quotes")).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/quotes").contentType(MediaType.APPLICATION_JSON).content(EXAMPLE_1))
				.andExpect(status().isUnauthorized());
	}

	// --- Helpers ---

	private String token(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"Demo2026!\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private ResultActions save(String email, String json) throws Exception {
		return mockMvc.perform(post("/api/quotes")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email))
				.contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private Integer savedId(String email, String json) throws Exception {
		String body = save(email, json).andExpect(status().isCreated()).andReturn().getResponse()
				.getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions read(String email, long id) throws Exception {
		return mockMvc.perform(get("/api/quotes/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email)));
	}

	private ResultActions list(String email) throws Exception {
		return mockMvc.perform(get("/api/quotes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email)));
	}

	private String statusInDatabase(Integer id) {
		return jdbcTemplate.queryForObject("SELECT status FROM quotes WHERE id = ?", String.class, id);
	}

}
