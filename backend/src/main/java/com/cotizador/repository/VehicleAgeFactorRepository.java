package com.cotizador.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.VehicleAgeFactor;

public interface VehicleAgeFactorRepository extends JpaRepository<VehicleAgeFactor, Long> {

	/** The row whose inclusive range contains the vehicle age; a null {@code maxYears} has no upper limit. */
	@Query("""
			SELECT f FROM VehicleAgeFactor f
			WHERE f.minYears <= :years AND (f.maxYears IS NULL OR f.maxYears >= :years)
			""")
	Optional<VehicleAgeFactor> findForAge(@Param("years") int years);

}
