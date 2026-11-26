package com.cotizador;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.cotizador.entity.Producer;
import com.cotizador.repository.AllowedUsageRepository;
import com.cotizador.repository.BrandRepository;
import com.cotizador.repository.CityRepository;
import com.cotizador.repository.ClientRepository;
import com.cotizador.repository.DriverAgeFactorRepository;
import com.cotizador.repository.InsurancePlanRepository;
import com.cotizador.repository.NumberSequenceRepository;
import com.cotizador.repository.PolicyRepository;
import com.cotizador.repository.ProducerRepository;
import com.cotizador.repository.ProvinceRepository;
import com.cotizador.repository.QuoteRepository;
import com.cotizador.repository.TariffZoneRepository;
import com.cotizador.repository.UsageFactorRepository;
import com.cotizador.repository.VehicleAgeFactorRepository;
import com.cotizador.repository.VehicleModelRepository;
import com.cotizador.repository.VehicleVersionRepository;

/** Stage 2: Flyway migrations, schema validation and seed data (AC-SETUP-02, AC-SETUP-03). */
@SpringBootTest
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, FixedClockConfiguration.class })
class DatabaseSetupIT {

	@Autowired
	private Flyway flyway;

	@Autowired
	private Environment environment;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private TariffZoneRepository tariffZoneRepository;

	@Autowired
	private ProvinceRepository provinceRepository;

	@Autowired
	private CityRepository cityRepository;

	@Autowired
	private InsurancePlanRepository insurancePlanRepository;

	@Autowired
	private UsageFactorRepository usageFactorRepository;

	@Autowired
	private AllowedUsageRepository allowedUsageRepository;

	@Autowired
	private VehicleAgeFactorRepository vehicleAgeFactorRepository;

	@Autowired
	private DriverAgeFactorRepository driverAgeFactorRepository;

	@Autowired
	private BrandRepository brandRepository;

	@Autowired
	private VehicleModelRepository vehicleModelRepository;

	@Autowired
	private VehicleVersionRepository vehicleVersionRepository;

	@Autowired
	private ProducerRepository producerRepository;

	@Autowired
	private ClientRepository clientRepository;

	@Autowired
	private QuoteRepository quoteRepository;

	@Autowired
	private PolicyRepository policyRepository;

	@Autowired
	private NumberSequenceRepository numberSequenceRepository;

	@Test
	void AC_SETUP_02_flywayAppliesV1ToV4Successfully() {
		List<MigrationInfo> applied = Arrays.asList(flyway.info().applied());

		assertThat(applied)
				.extracting(info -> info.getVersion().getVersion())
				.containsExactly("1", "2", "3", "4");
		assertThat(applied)
				.allSatisfy(info -> assertThat(info.getState()).isEqualTo(MigrationState.SUCCESS));
	}

	@Test
	void AC_SETUP_02_contextStartsWithDdlAutoValidate() {
		// The Spring context (and therefore Hibernate schema validation) started for this test to run.
		assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
	}

	@Test
	void AC_SETUP_02_seedDataCountsMatchSpec() {
		assertThat(tariffZoneRepository.count()).isEqualTo(7);
		assertThat(cityRepository.count()).isEqualTo(10);
		assertThat(insurancePlanRepository.count()).isEqualTo(3);
		assertThat(brandRepository.count()).isEqualTo(12);
		assertThat(vehicleModelRepository.count()).isEqualTo(24);
		assertThat(vehicleVersionRepository.count()).isEqualTo(48);
		assertThat(producerRepository.count()).isEqualTo(2);
		assertThat(clientRepository.count()).isEqualTo(6);

		// Remaining seed tables (seed-data.md) and tables that start empty.
		assertThat(provinceRepository.count()).isEqualTo(5);
		assertThat(usageFactorRepository.count()).isEqualTo(7);
		assertThat(allowedUsageRepository.count()).isEqualTo(17);
		assertThat(vehicleAgeFactorRepository.count()).isEqualTo(4);
		assertThat(driverAgeFactorRepository.count()).isEqualTo(5);
		assertThat(quoteRepository.count()).isZero();
		assertThat(policyRepository.count()).isZero();
		assertThat(numberSequenceRepository.count()).isZero();
	}

	@Test
	void AC_SETUP_02_identitySequencesContinueAfterSeedIds() {
		assertThat(lastSequenceValue("tariff_zones")).isEqualTo(7);
		assertThat(lastSequenceValue("provinces")).isEqualTo(5);
		assertThat(lastSequenceValue("cities")).isEqualTo(10);
		assertThat(lastSequenceValue("insurance_plans")).isEqualTo(3);
		assertThat(lastSequenceValue("vehicle_age_factors")).isEqualTo(4);
		assertThat(lastSequenceValue("driver_age_factors")).isEqualTo(5);
		assertThat(lastSequenceValue("brands")).isEqualTo(12);
		assertThat(lastSequenceValue("models")).isEqualTo(24);
		assertThat(lastSequenceValue("versions")).isEqualTo(48);
		assertThat(lastSequenceValue("producers")).isEqualTo(2);
		assertThat(lastSequenceValue("clients")).isEqualTo(6);
	}

	@Test
	void AC_SETUP_03_seededProducersPasswordHashMatchesDemoPassword() {
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		List<Producer> producers = producerRepository.findAll();

		assertThat(producers).hasSize(2);
		assertThat(producers).allSatisfy(producer -> {
			assertThat(encoder.matches("Demo2026!", producer.getPasswordHash())).isTrue();
			assertThat(producer.getEmail()).isEqualTo(producer.getEmail().toLowerCase());
		});
	}

	/** Reads the identity sequence's last value without consuming it. */
	private Long lastSequenceValue(String table) {
		return jdbcTemplate.queryForObject(
				"SELECT s.last_value FROM pg_sequences s "
						+ "WHERE s.schemaname || '.' || s.sequencename = pg_get_serial_sequence(?, 'id')",
				Long.class, table);
	}

}
