package com.cotizador.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.Quote;

/** Quotes are always filtered by the producer of the token in the query (backend.md §7.4, rule Q5). */
public interface QuoteRepository extends JpaRepository<Quote, Long> {

	/** Quotes of a producer, most recent first (api.md §8.4). Loads the client, used for {@code clientName}. */
	@EntityGraph(attributePaths = { "client" })
	List<Quote> findByProducerIdOrderByCreatedAtDescIdDesc(Long producerId);

	/** A quote of the producer; empty when it does not exist or belongs to another producer. */
	@EntityGraph(attributePaths = { "client", "client.city", "client.city.province" })
	Optional<Quote> findByIdAndProducerId(Long id, Long producerId);

}
