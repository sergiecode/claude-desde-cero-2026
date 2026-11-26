package com.cotizador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.VehicleVersion;

public interface VehicleVersionRepository extends JpaRepository<VehicleVersion, Long> {

	List<VehicleVersion> findByModelIdOrderByIdAsc(Long modelId);

}
