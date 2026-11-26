package com.cotizador.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.NumberSequence;
import com.cotizador.entity.NumberSequenceId;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, NumberSequenceId> {

	/** {@code SELECT ... FOR UPDATE}: locks the counter row until the caller's transaction ends (backend.md §6). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<NumberSequence> findByPrefixAndYear(String prefix, Integer year);

	/**
	 * Creates the counter of the year with value 1. Returns 0 if another transaction created it first; the caller
	 * then retries the locked select. {@code ON CONFLICT DO NOTHING} keeps the transaction usable in that case.
	 */
	@Modifying
	@Query(value = "INSERT INTO number_sequences (prefix, year, last_value) VALUES (:prefix, :year, 1) "
			+ "ON CONFLICT (prefix, year) DO NOTHING", nativeQuery = true)
	int insertFirst(@Param("prefix") String prefix, @Param("year") int year);

}
