package com.cotizador.exception;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Converts every error into Problem Details with {@code code} and {@code errors} (backend.md §10, api.md §2). */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	static final String REQUIRED_FIELD_MESSAGE = "Completá este campo";

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex) {
		return respond(ProblemDetails.of(ex.getCode(), ex.getMessage(), List.of()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
		List<FieldErrorResponse> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(this::toFieldError)
				.toList();
		return validationError(errors);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ProblemDetail> handleMissingParameter(MissingServletRequestParameterException ex) {
		return validationError(List.of(new FieldErrorResponse(ex.getParameterName(), REQUIRED_FIELD_MESSAGE)));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return validationError(List.of(new FieldErrorResponse(ex.getName(), ErrorCode.VALIDATION_ERROR.message())));
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException ex) {
		List<FieldErrorResponse> errors = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new FieldErrorResponse(
								result.getMethodParameter().getParameterName(),
								error.getDefaultMessage())))
				.toList();
		return validationError(errors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException ex) {
		return validationError(List.of());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
		if (ex instanceof ErrorResponse errorResponse && errorResponse.getStatusCode().is4xxClientError()) {
			// Framework errors not covered by api.md §2.1 (unknown route, method not allowed, unsupported media type)
			// keep their HTTP status instead of becoming a 500.
			ProblemDetail problem = errorResponse.getBody();
			problem.setProperty(ProblemDetails.ERRORS, List.of());
			return respond(problem);
		}
		log.error("Unexpected error", ex);
		return respond(ProblemDetails.of(ErrorCode.INTERNAL_ERROR));
	}

	private FieldErrorResponse toFieldError(FieldError error) {
		return new FieldErrorResponse(error.getField(), error.getDefaultMessage());
	}

	private ResponseEntity<ProblemDetail> validationError(List<FieldErrorResponse> errors) {
		return respond(ProblemDetails.of(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.message(), errors));
	}

	private ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
		return ResponseEntity.status(problem.getStatus()).body(problem);
	}

}
