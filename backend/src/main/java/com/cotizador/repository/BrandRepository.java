package com.cotizador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.Brand;
import com.cotizador.entity.VehicleType;

public interface BrandRepository extends JpaRepository<Brand, Long> {

	/** Brands of the type with at least one model available for the year (rule V4), ordered by name. */
	@Query("""
			SELECT b FROM Brand b
			WHERE b.vehicleType = :vehicleType
			  AND EXISTS (SELECT 1 FROM VehicleModel m WHERE m.brand = b AND m.availableFromYear <= :year)
			ORDER BY b.name, b.id
			""")
	List<Brand> findAvailable(@Param("vehicleType") VehicleType vehicleType, @Param("year") int year);

}
