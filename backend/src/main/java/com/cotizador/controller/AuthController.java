package com.cotizador.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cotizador.dto.LoginRequest;
import com.cotizador.dto.LoginResponse;
import com.cotizador.dto.ProducerResponse;
import com.cotizador.security.CurrentProducer;
import com.cotizador.service.AuthService;

/** Authentication endpoints (api.md §3). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final CurrentProducer currentProducer;

	public AuthController(AuthService authService, CurrentProducer currentProducer) {
		this.authService = authService;
		this.currentProducer = currentProducer;
	}

	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@GetMapping("/me")
	public ProducerResponse me() {
		return authService.me(currentProducer.id());
	}

}
