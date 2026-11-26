package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Producer;

public interface ProducerRepository extends JpaRepository<Producer, Long> {

	/** Emails are stored in lowercase; callers pass the lowercase email. */
	Optional<Producer> findByEmail(String email);

}
