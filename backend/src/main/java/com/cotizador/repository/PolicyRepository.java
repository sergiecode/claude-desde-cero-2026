package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Policy;

/** Policies are always filtered by the producer of the token in the query (backend.md §7.4, rule E10). */
public interface PolicyRepository extends JpaRepository<Policy, Long> {

	/** A policy of the producer; empty when it does not exist or belongs to another producer. */
	@EntityGraph(attributePaths = { "quote", "client", "client.city", "client.city.province" })
	Optional<Policy> findByIdAndProducerId(Long id, Long producerId);

	/** The policy issued from a quote of the producer (at most one, rule E2). */
	Optional<Policy> findByQuoteIdAndProducerId(Long quoteId, Long producerId);

}
