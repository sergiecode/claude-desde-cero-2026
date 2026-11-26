package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.VehicleVersion;

public interface VehicleVersionRepository extends JpaRepository<VehicleVersion, Long> {

}
