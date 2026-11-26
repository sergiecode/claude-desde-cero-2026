package com.cotizador.exception;

/** Single exception for business errors (backend.md §10). */
public class BusinessException extends RuntimeException {

	private final ErrorCode code;

	public BusinessException(ErrorCode code, Object... args) {
		super(code.message(args));
		this.code = code;
	}

	public ErrorCode getCode() {
		return code;
	}

}
