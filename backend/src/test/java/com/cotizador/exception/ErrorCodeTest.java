package com.cotizador.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class ErrorCodeTest {

	@Test
	void authCodesHaveStatusAndMessageFromApiSpec() {
		assertThat(ErrorCode.INVALID_CREDENTIALS.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(ErrorCode.INVALID_CREDENTIALS.message()).isEqualTo("Usuario o contraseña incorrectos");
		assertThat(ErrorCode.UNAUTHORIZED.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(ErrorCode.UNAUTHORIZED.message()).isEqualTo("La sesión expiró. Ingresá nuevamente");
	}

	@Test
	void messageFillsPlaceholders() {
		assertThat(new BusinessException(ErrorCode.QUOTE_ALREADY_ISSUED, "POL-2026-000001").getMessage())
				.isEqualTo("Esta cotización ya fue emitida (póliza POL-2026-000001)");
	}

	@Test
	void problemDetailsCarryCodeAndEmptyErrors() {
		ProblemDetail problem = ProblemDetails.of(ErrorCode.INTERNAL_ERROR);

		assertThat(problem.getStatus()).isEqualTo(500);
		assertThat(problem.getDetail()).isEqualTo("Ocurrió un error inesperado. Intentá nuevamente");
		assertThat(problem.getProperties())
				.containsEntry("code", "INTERNAL_ERROR")
				.containsEntry("errors", List.of());
	}

}
