package com.cotizador.exception;

import java.net.URI;
import java.util.List;

import org.springframework.http.ProblemDetail;

/** Builds Problem Details (RFC 9457) with the extra {@code code} and {@code errors} fields (api.md §2). */
public final class ProblemDetails {

	public static final String CODE = "code";
	public static final String ERRORS = "errors";

	private ProblemDetails() {
	}

	public static ProblemDetail of(ErrorCode code, String detail, List<FieldErrorResponse> errors) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
		problem.setType(URI.create("about:blank"));
		problem.setProperty(CODE, code.name());
		problem.setProperty(ERRORS, errors);
		return problem;
	}

	public static ProblemDetail of(ErrorCode code) {
		return of(code, code.message(), List.of());
	}

}
