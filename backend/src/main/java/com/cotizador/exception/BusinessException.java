package com.cotizador.exception;

import java.util.List;

/** Single exception for business errors (backend.md §10). */
public class BusinessException extends RuntimeException {

	private final ErrorCode code;
	private final List<FieldErrorResponse> errors;

	public BusinessException(ErrorCode code, Object... args) {
		this(code, List.of(), args);
	}

	private BusinessException(ErrorCode code, List<FieldErrorResponse> errors, Object... args) {
		super(code.message(args));
		this.code = code;
		this.errors = errors;
	}

	/**
	 * A business error caused by one request field: the response's {@code errors} list has that field with the
	 * same message as {@code detail} (api.md §2).
	 */
	public static BusinessException forField(ErrorCode code, String field, Object... args) {
		String message = code.message(args);
		return new BusinessException(code, List.of(new FieldErrorResponse(field, message)), args);
	}

	public ErrorCode getCode() {
		return code;
	}

	/** Field errors for the {@code errors} list; empty unless the error belongs to a field. */
	public List<FieldErrorResponse> getErrors() {
		return errors;
	}

}
