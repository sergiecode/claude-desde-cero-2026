package com.cotizador.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cotizador.dto.LoginRequest;
import com.cotizador.dto.LoginResponse;
import com.cotizador.dto.ProducerResponse;
import com.cotizador.entity.Producer;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.mapper.ProducerMapper;
import com.cotizador.repository.ProducerRepository;
import com.cotizador.security.JwtService;

/** Producer login and session data (backend.md §9, api.md §3). */
@Service
public class AuthService {

	private final ProducerRepository producerRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final ProducerMapper producerMapper;

	public AuthService(ProducerRepository producerRepository, PasswordEncoder passwordEncoder,
			JwtService jwtService, ProducerMapper producerMapper) {
		this.producerRepository = producerRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.producerMapper = producerMapper;
	}

	/** Any failure (unknown email, inactive producer, wrong password) gives the same INVALID_CREDENTIALS error. */
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		Producer producer = producerRepository.findByEmail(request.email().toLowerCase(Locale.ROOT))
				.filter(Producer::isActive)
				.filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		JwtService.IssuedToken issued = jwtService.issue(producer.getId());
		return new LoginResponse(issued.token(), issued.expiresAt(), producerMapper.toResponse(producer));
	}

	/** The producer of the token; a token whose producer no longer exists is treated as an invalid session. */
	@Transactional(readOnly = true)
	public ProducerResponse me(Long producerId) {
		return producerRepository.findById(producerId)
				.map(producerMapper::toResponse)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

}
