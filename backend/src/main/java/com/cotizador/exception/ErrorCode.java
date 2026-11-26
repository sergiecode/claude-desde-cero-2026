package com.cotizador.exception;

import org.springframework.http.HttpStatus;

/**
 * Stable error codes with their HTTP status and user-facing message (api.md §2.1).
 * Messages use {@link String#format} placeholders for variable values.
 */
public enum ErrorCode {

	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "La sesión expiró. Ingresá nuevamente"),
	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Revisá los datos ingresados"),
	INVALID_DOCUMENT(HttpStatus.BAD_REQUEST, "El número de documento no es válido"),
	CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Cliente no encontrado"),
	CLIENT_UNDERAGE(HttpStatus.UNPROCESSABLE_CONTENT, "El cliente debe ser mayor de 18 años"),
	INVALID_YEAR(HttpStatus.BAD_REQUEST, "El año debe estar entre 2010 y %s"),
	INVALID_VEHICLE(HttpStatus.BAD_REQUEST, "La marca, el modelo o la versión no corresponden al tipo y año elegidos"),
	USAGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "El tipo de uso no está permitido para este vehículo"),
	INVALID_PATENT(HttpStatus.BAD_REQUEST, "La patente debe tener el formato ABC123"),
	VALUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "No hay valuación disponible para este vehículo"),
	INSURED_AMOUNT_OUT_OF_RANGE(HttpStatus.BAD_REQUEST, "La suma asegurada debe estar entre $ %s y $ %s"),
	PLAN_NOT_FOUND(HttpStatus.BAD_REQUEST, "El plan seleccionado no existe"),
	QUOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "Cotización no encontrada"),
	QUOTE_EXPIRED(HttpStatus.CONFLICT, "La cotización está vencida. Generá una nueva"),
	QUOTE_ALREADY_ISSUED(HttpStatus.CONFLICT, "Esta cotización ya fue emitida (póliza %s)"),
	INVALID_START_DATE(HttpStatus.BAD_REQUEST, "La fecha de inicio debe estar entre hoy y los próximos 30 días"),
	POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "Póliza no encontrada"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado. Intentá nuevamente");

	private final HttpStatus status;
	private final String message;

	ErrorCode(HttpStatus status, String message) {
		this.status = status;
		this.message = message;
	}

	public HttpStatus status() {
		return status;
	}

	/** The user-facing message with the given values in its placeholders. */
	public String message(Object... args) {
		return args.length == 0 ? message : String.format(message, args);
	}

}
