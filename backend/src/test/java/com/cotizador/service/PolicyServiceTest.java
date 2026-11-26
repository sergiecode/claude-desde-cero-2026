package com.cotizador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.PolicyMapper;
import com.cotizador.repository.PolicyRepository;

/** Orchestration of {@link PolicyService#issue}: expiration first, then the locked issue (backend.md §7.3). */
class PolicyServiceTest {

	private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-11-19T10:00:00-03:00");
	private static final Clock CLOCK = Clock.fixed(NOW.toInstant(), ZoneId.of("America/Argentina/Buenos_Aires"));
	private static final LocalDate START = LocalDate.parse("2026-11-20");

	private QuoteExpirationService expirationService;
	private PolicyIssuer issuer;
	private PolicyService service;

	@BeforeEach
	void setUp() {
		expirationService = mock(QuoteExpirationService.class);
		issuer = mock(PolicyIssuer.class);
		service = new PolicyService(expirationService, issuer, mock(PolicyRepository.class), mock(PolicyMapper.class),
				CLOCK);
	}

	@Test
	void AC_POL_08_quoteExpiredByThePreviousStepIsNotIssued() {
		when(expirationService.expireIfOverdue(1L, 1L, NOW)).thenReturn(true);

		assertThatThrownBy(() -> service.issue(1L, START, 1L))
				.isInstanceOfSatisfying(BusinessException.class,
						ex -> assertThat(ex.getCode()).isEqualTo(ErrorCode.QUOTE_EXPIRED));
		verify(issuer, never()).issue(anyLong(), any(), anyLong(), any());
	}

	@Test
	void AC_POL_07_uniqueViolationIsAnsweredAsAlreadyIssued() {
		when(expirationService.expireIfOverdue(1L, 1L, NOW)).thenReturn(false);
		when(issuer.issue(eq(1L), eq(START), eq(1L), any())).thenThrow(new DataIntegrityViolationException("dup"));
		BusinessException alreadyIssued = new BusinessException(ErrorCode.QUOTE_ALREADY_ISSUED, "POL-2026-000001");
		when(issuer.alreadyIssued(1L, 1L)).thenReturn(alreadyIssued);

		assertThatThrownBy(() -> service.issue(1L, START, 1L))
				.isSameAs(alreadyIssued)
				.hasMessage("Esta cotización ya fue emitida (póliza POL-2026-000001)");
	}

}
