package com.cotizador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cotizador.entity.VehicleModel;
import com.cotizador.entity.VehicleType;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

	/** Models of the brand (which must be of the type) available for the year (rule V4), ordered by name. */
	@Query("""
			SELECT m FROM VehicleModel m
			WHERE m.brand.id = :brandId
			  AND m.brand.vehicleType = :vehicleType
			  AND m.availableFromYear <= :year
			ORDER BY m.name, m.id
			""")
	List<VehicleModel> findAvailable(@Param("vehicleType") VehicleType vehicleType, @Param("year") int year,
			@Param("brandId") long brandId);

}
