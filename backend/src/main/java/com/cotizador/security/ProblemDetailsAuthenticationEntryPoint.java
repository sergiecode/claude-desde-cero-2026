package com.cotizador.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.cotizador.exception.ErrorCode;
import com.cotizador.exception.ProblemDetails;

import tools.jackson.databind.json.JsonMapper;

/**
 * Missing, invalid or expired token: {@code 401 UNAUTHORIZED} in Problem Details (backend.md §9, api.md §2).
 */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final JsonMapper jsonMapper;

	public ProblemDetailsAuthenticationEntryPoint(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorCode code = ErrorCode.UNAUTHORIZED;
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("type", "about:blank");
		body.put("title", code.status().getReasonPhrase());
		body.put("status", code.status().value());
		body.put("detail", code.message());
		body.put("instance", request.getRequestURI());
		body.put(ProblemDetails.CODE, code.name());
		body.put(ProblemDetails.ERRORS, List.of());

		response.setStatus(code.status().value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("WWW-Authenticate", "Bearer");
		jsonMapper.writeValue(response.getOutputStream(), body);
	}

}
