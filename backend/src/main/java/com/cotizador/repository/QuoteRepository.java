package com.cotizador.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.Quote;
import com.cotizador.entity.QuoteStatus;

/** Quotes are always filtered by the producer of the token in the query (backend.md §7.4, rule Q5). */
public interface QuoteRepository extends JpaRepository<Quote, Long> {

	/** Quotes of a producer, most recent first (api.md §8.4). Loads the client, used for {@code clientName}. */
	@EntityGraph(attributePaths = { "client" })
	List<Quote> findByProducerIdOrderByCreatedAtDescIdDesc(Long producerId);

	/** A quote of the producer; empty when it does not exist or belongs to another producer. */
	@EntityGraph(attributePaths = { "client", "client.city", "client.city.province" })
	Optional<Quote> findByIdAndProducerId(Long id, Long producerId);

	/**
	 * A quote of the producer with {@code SELECT ... FOR UPDATE} (backend.md §7.3): the row stays locked until the
	 * caller's transaction ends, so two issues of the same quote run one after the other. No joins are fetched,
	 * because PostgreSQL does not lock the nullable side of an outer join.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT q FROM Quote q WHERE q.id = :id AND q.producer.id = :producerId")
	Optional<Quote> lockByIdAndProducerId(@Param("id") Long id, @Param("producerId") Long producerId);

	/**
	 * Expiration on issue (backend.md §7.2) as a single conditional update: a quote of the producer that is still
	 * {@code from} and whose validity ended before {@code now} changes to {@code to}. Returns the updated rows (0 or 1).
	 */
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Quote q SET q.status = :to WHERE q.id = :id AND q.producer.id = :producerId "
			+ "AND q.status = :from AND q.validUntil < :now")
	int expireIfOverdue(@Param("id") Long id, @Param("producerId") Long producerId, @Param("now") OffsetDateTime now,
			@Param("from") QuoteStatus from, @Param("to") QuoteStatus to);

}
