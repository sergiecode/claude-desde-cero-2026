package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.DriverAgeFactor;

public interface DriverAgeFactorRepository extends JpaRepository<DriverAgeFactor, Long> {

	/** The row whose inclusive range contains the driver age; a null {@code maxAge} has no upper limit. */
	@Query("""
			SELECT f FROM DriverAgeFactor f
			WHERE f.minAge <= :age AND (f.maxAge IS NULL OR f.maxAge >= :age)
			""")
	Optional<DriverAgeFactor> findForAge(@Param("age") int age);

}
