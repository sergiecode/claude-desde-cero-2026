package com.cotizador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/**
 * Stage 11: issuing and reading policies (AC-POL-01 to AC-POL-14). The clock starts at 2026-11-19T10:00:00-03:00
 * and is moved only by the criteria that need another date. Uses the same context as {@link QuoteIT}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, QuoteIT.MutableClockConfiguration.class })
class PolicyIT {

	static final OffsetDateTime NOW = QuoteIT.NOW;

	/** One second after the {@code valid_until} of a quote saved at {@link #NOW} (15 days later). */
	static final OffsetDateTime AFTER_VALIDITY = OffsetDateTime.parse("2026-12-04T10:00:01-03:00");

	private static final String ANA = "ana.torres@aseguradora.test";
	private static final String DIEGO = "diego.sosa@aseguradora.test";

	/** AC-QUO-01 / AC-PRICE-01: Lucía Fernández, VW Gol Base 2018, Particular, Terceros completo, 10.800.000. */
	private static final String EXAMPLE_1 = """
			{"clientId":1,"vehicleType":"AUTO","manufacturingYear":2018,"brandId":2,"modelId":3,"versionId":5,
			 "usageType":"PARTICULAR","patent":"abc123","planId":2,"insuredAmount":10800000.00}""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MutableClock clock;

	@BeforeEach
	void cleanData() {
		clock.setTo(NOW);
		jdbcTemplate.update("DELETE FROM policies");
		jdbcTemplate.update("DELETE FROM quotes");
		jdbcTemplate.update("DELETE FROM number_sequences");
	}

	@AfterEach
	void restoreClock() {
		clock.setTo(NOW);
	}

	@Test
	void AC_POL_01_issueCreatesAnActivePolicyWithTheAmountsOfTheQuote() throws Exception {
		Integer quoteId = savedQuote(ANA);

		String body = issue(ANA, quoteId, "2026-11-20")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.policyNumber").value("POL-2026-000001"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.issuedAt").value("2026-11-19T10:00:00-03:00"))
				.andExpect(jsonPath("$.startDate").value("2026-11-20"))
				.andExpect(jsonPath("$.endDate").value("2027-11-20"))
				.andExpect(jsonPath("$.quoteId").value(quoteId))
				.andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"))
				.andExpect(jsonPath("$.client.id").value(1))
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
				.andExpect(jsonPath("$.insuredAmount").value(10800000.00))
				.andExpect(jsonPath("$.premium").value(46332.00))
				.andExpect(jsonPath("$.taxes").value(9729.72))
				.andExpect(jsonPath("$.totalPrice").value(56061.72))
				.andExpect(jsonPath("$.currency").value("ARS"))
				.andExpect(content().string(containsString("\"totalPrice\":56061.72")))
				.andReturn().getResponse().getContentAsString();

		Integer policyId = JsonPath.read(body, "$.id");
		assertThat(quoteStatus(quoteId)).isEqualTo("EMITTED");

		Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM policies WHERE id = ?", policyId);
		assertThat(row.get("policy_number")).isEqualTo("POL-2026-000001");
		assertThat(((Number) row.get("quote_id")).intValue()).isEqualTo(quoteId);
		assertThat(row.get("producer_id")).isEqualTo(1L);
		assertThat(row.get("client_id")).isEqualTo(1L);
		assertThat(row.get("status")).isEqualTo("ACTIVE");
		assertThat(row.get("start_date").toString()).isEqualTo("2026-11-20");
		assertThat(row.get("end_date").toString()).isEqualTo("2027-11-20");
		assertThat((BigDecimal) row.get("insured_amount")).isEqualByComparingTo("10800000.00");
		assertThat((BigDecimal) row.get("premium")).isEqualByComparingTo("46332.00");
		assertThat((BigDecimal) row.get("taxes")).isEqualByComparingTo("9729.72");
		assertThat((BigDecimal) row.get("total_price")).isEqualByComparingTo("56061.72");
		assertThat(row.get("currency")).isEqualTo("ARS");
	}

	@Test
	void AC_POL_01_locationHeaderPointsToThePolicy() throws Exception {
		Integer quoteId = savedQuote(ANA);

		ResultActions result = issue(ANA, quoteId, "2026-11-20").andExpect(status().isCreated());
		Integer policyId = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");

		result.andExpect(header().string(HttpHeaders.LOCATION, "/api/policies/" + policyId));
		readPolicy(ANA, policyId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(policyId))
				.andExpect(jsonPath("$.policyNumber").value("POL-2026-000001"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.issuedAt").value("2026-11-19T10:00:00-03:00"))
				.andExpect(jsonPath("$.startDate").value("2026-11-20"))
				.andExpect(jsonPath("$.endDate").value("2027-11-20"))
				.andExpect(jsonPath("$.quoteId").value(quoteId))
				.andExpect(jsonPath("$.quoteNumber").value("COT-2026-000001"))
				.andExpect(jsonPath("$.client.firstName").value("Lucía"))
				.andExpect(jsonPath("$.vehicle.patent").value("ABC123"))
				.andExpect(jsonPath("$.planName").value("Terceros completo"))
				.andExpect(jsonPath("$.totalPrice").value(56061.72))
				.andExpect(jsonPath("$.currency").value("ARS"));
	}

	@Test
	void AC_POL_02_startDateTodayIsAccepted() throws Exception {
		Integer quoteId = savedQuote(ANA);

		issue(ANA, quoteId, "2026-11-19")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startDate").value("2026-11-19"))
				.andExpect(jsonPath("$.endDate").value("2027-11-19"));
	}

	@Test
	void AC_POL_03_startDateThirtyDaysAheadIsAccepted() throws Exception {
		Integer quoteId = savedQuote(ANA);

		issue(ANA, quoteId, "2026-12-19")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startDate").value("2026-12-19"))
				.andExpect(jsonPath("$.endDate").value("2027-12-19"));
	}

	@Test
	void AC_POL_04_startDateInThePastIsRejected() throws Exception {
		Integer quoteId = savedQuote(ANA);

		expectInvalidStartDate(issue(ANA, quoteId, "2026-11-18"));
		assertNothingIssued(quoteId);
	}

	@Test
	void AC_POL_05_startDateMoreThanThirtyDaysAheadIsRejected() throws Exception {
		Integer quoteId = savedQuote(ANA);

		expectInvalidStartDate(issue(ANA, quoteId, "2026-12-20"));
		assertNothingIssued(quoteId);
	}

	@Test
	void AC_POL_06_issuingTwiceIsRejectedWithThePolicyNumber() throws Exception {
		Integer quoteId = savedQuote(ANA);
		issue(ANA, quoteId, "2026-11-20").andExpect(status().isCreated());

		issue(ANA, quoteId, "2026-11-20")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("QUOTE_ALREADY_ISSUED"))
				.andExpect(jsonPath("$.detail").value("Esta cotización ya fue emitida (póliza POL-2026-000001)"));

		assertThat(policiesOf(quoteId)).isOne();
	}

	@Test
	@Timeout(value = 60, unit = TimeUnit.SECONDS)
	void AC_POL_07_simultaneousIssuesCreateASinglePolicy() throws Exception {
		Integer quoteId = savedQuote(ANA);
		String token = token(ANA);

		int requests = 2;
		CountDownLatch ready = new CountDownLatch(requests);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(requests);
		try {
			List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
			for (int i = 0; i < requests; i++) {
				Callable<MockHttpServletResponse> call = () -> {
					ready.countDown();
					start.await();
					return mockMvc.perform(post("/api/quotes/{id}/issue", quoteId)
							.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON)
							.content("{\"startDate\":\"2026-11-20\"}"))
							.andReturn().getResponse();
				};
				futures.add(executor.submit(call));
			}
			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
			start.countDown();

			List<MockHttpServletResponse> responses = new ArrayList<>();
			for (Future<MockHttpServletResponse> future : futures) {
				responses.add(future.get(30, TimeUnit.SECONDS));
			}

			assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
			MockHttpServletResponse conflict = responses.stream().filter(r -> r.getStatus() == 409).findFirst()
					.orElseThrow();
			String conflictBody = conflict.getContentAsString();
			assertThat((String) JsonPath.read(conflictBody, "$.code")).isEqualTo("QUOTE_ALREADY_ISSUED");
			assertThat((String) JsonPath.read(conflictBody, "$.detail"))
					.isEqualTo("Esta cotización ya fue emitida (póliza POL-2026-000001)");
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(policiesOf(quoteId)).isOne();
		assertThat(quoteStatus(quoteId)).isEqualTo("EMITTED");
	}

	@Test
	void AC_POL_08_expiredQuoteIsNotIssuedAndStaysExpired() throws Exception {
		Integer quoteId = savedQuote(ANA);

		clock.setTo(AFTER_VALIDITY);

		issue(ANA, quoteId, "2026-12-05")
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"))
				.andExpect(jsonPath("$.detail").value("La cotización está vencida. Generá una nueva"));

		assertThat(quoteStatus(quoteId)).isEqualTo("EXPIRED");
		assertThat(policiesOf(quoteId)).isZero();

		// Already EXPIRED in the database: a new attempt is refused the same way.
		issue(ANA, quoteId, "2026-12-05")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("QUOTE_EXPIRED"));
		assertThat(policiesOf(quoteId)).isZero();
	}

	@Test
	void AC_POL_08_quoteIsStillIssuedAtItsValidUntil() throws Exception {
		Integer quoteId = savedQuote(ANA);

		clock.setTo(OffsetDateTime.parse("2026-12-04T10:00:00-03:00"));

		issue(ANA, quoteId, "2026-12-04").andExpect(status().isCreated());
		assertThat(quoteStatus(quoteId)).isEqualTo("EMITTED");
	}

	@Test
	void AC_POL_09_producerCannotIssueAQuoteOfAnotherProducer() throws Exception {
		Integer quoteId = savedQuote(ANA);

		issue(DIEGO, quoteId, "2026-11-20")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Cotización no encontrada"));

		assertNothingIssued(quoteId);
	}

	@Test
	void AC_POL_09_overdueQuoteOfAnotherProducerIsNotFoundAndNotExpired() throws Exception {
		Integer quoteId = savedQuote(ANA);

		clock.setTo(AFTER_VALIDITY);

		issue(DIEGO, quoteId, "2026-12-05")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
		assertThat(quoteStatus(quoteId)).isEqualTo("CALCULATED");
	}

	@Test
	void AC_POL_09_unknownQuoteIsNotFound() throws Exception {
		issue(ANA, 999999, "2026-11-20")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"));
	}

	@Test
	void AC_POL_10_producerCannotReadAPolicyOfAnotherProducer() throws Exception {
		Integer policyId = issuedPolicy(ANA, "2026-11-20");

		readPolicy(DIEGO, policyId)
				.andExpect(status().isNotFound())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"))
				.andExpect(jsonPath("$.detail").value("Póliza no encontrada"));
	}

	@Test
	void AC_POL_10_unknownPolicyIsNotFound() throws Exception {
		readPolicy(ANA, 999999)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
	}

	@Test
	void AC_POL_11_policyIsStillActiveOnItsEndDate() throws Exception {
		Integer policyId = issuedPolicy(ANA, "2026-11-20");

		clock.setTo(OffsetDateTime.parse("2027-11-20T23:59:59-03:00"));

		readPolicy(ANA, policyId).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
		assertThat(policyStatus(policyId)).isEqualTo("ACTIVE");
	}

	@Test
	void AC_POL_12_policyExpiresTheDayAfterItsEndDateAndIsSaved() throws Exception {
		Integer policyId = issuedPolicy(ANA, "2026-11-20");

		clock.setTo(OffsetDateTime.parse("2027-11-21T00:00:00-03:00"));

		readPolicy(ANA, policyId).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
		assertThat(policyStatus(policyId)).isEqualTo("EXPIRED");
	}

	@Test
	void AC_POL_13_issuedQuoteShowsItsPolicy() throws Exception {
		Integer quoteId = savedQuote(ANA);
		Integer policyId = JsonPath.read(issue(ANA, quoteId, "2026-11-20")
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString(), "$.id");

		readQuote(ANA, quoteId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EMITTED"))
				.andExpect(jsonPath("$.policy.id").value(policyId))
				.andExpect(jsonPath("$.policy.policyNumber").value("POL-2026-000001"));
	}

	@Test
	void AC_POL_14_issuedQuoteNeverExpires() throws Exception {
		Integer quoteId = savedQuote(ANA);
		issue(ANA, quoteId, "2026-11-20").andExpect(status().isCreated());

		clock.setTo(AFTER_VALIDITY);

		readQuote(ANA, quoteId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("EMITTED"))
				.andExpect(jsonPath("$.policy.policyNumber").value("POL-2026-000001"));
		mockMvc.perform(get("/api/quotes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ANA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("EMITTED"));
		assertThat(quoteStatus(quoteId)).isEqualTo("EMITTED");

		// Issuing it again after its validity still answers that it was issued, not that it expired.
		issue(ANA, quoteId, "2026-12-05")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("QUOTE_ALREADY_ISSUED"));
		assertThat(quoteStatus(quoteId)).isEqualTo("EMITTED");
	}

	@Test
	void issueWithoutStartDateIsAValidationError() throws Exception {
		Integer quoteId = savedQuote(ANA);

		mockMvc.perform(post("/api/quotes/{id}/issue", quoteId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(ANA))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.errors[0].field").value("startDate"))
				.andExpect(jsonPath("$.errors[0].message").value("Completá este campo"));

		assertNothingIssued(quoteId);
	}

	@Test
	void policiesRequireAToken() throws Exception {
		mockMvc.perform(get("/api/policies/{id}", 1)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(post("/api/quotes/{id}/issue", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"startDate\":\"2026-11-20\"}"))
				.andExpect(status().isUnauthorized());
	}

	// --- Helpers ---

	private void expectInvalidStartDate(ResultActions result) throws Exception {
		result.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_START_DATE"))
				.andExpect(jsonPath("$.detail").value("La fecha de inicio debe estar entre hoy y los próximos 30 días"));
	}

	private void assertNothingIssued(Integer quoteId) {
		assertThat(quoteStatus(quoteId)).isEqualTo("CALCULATED");
		assertThat(policiesOf(quoteId)).isZero();
	}

	private String token(String email) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"" + email + "\",\"password\":\"Demo2026!\"}"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.token");
	}

	private Integer savedQuote(String email) throws Exception {
		String body = mockMvc.perform(post("/api/quotes")
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email))
				.contentType(MediaType.APPLICATION_JSON)
				.content(EXAMPLE_1))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private Integer issuedPolicy(String email, String startDate) throws Exception {
		Integer quoteId = savedQuote(email);
		String body = issue(email, quoteId, startDate).andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions issue(String email, long quoteId, String startDate) throws Exception {
		return mockMvc.perform(post("/api/quotes/{id}/issue", quoteId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"startDate\":\"" + startDate + "\"}"));
	}

	private ResultActions readPolicy(String email, long id) throws Exception {
		return mockMvc.perform(get("/api/policies/{id}", id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email)));
	}

	private ResultActions readQuote(String email, long id) throws Exception {
		return mockMvc.perform(get("/api/quotes/{id}", id)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token(email)));
	}

	private String quoteStatus(Integer quoteId) {
		return jdbcTemplate.queryForObject("SELECT status FROM quotes WHERE id = ?", String.class, quoteId);
	}

	private String policyStatus(Integer policyId) {
		return jdbcTemplate.queryForObject("SELECT status FROM policies WHERE id = ?", String.class, policyId);
	}

	private int policiesOf(Integer quoteId) {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM policies WHERE quote_id = ?", Integer.class, quoteId);
	}

}
