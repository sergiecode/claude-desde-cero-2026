package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.VehicleModel;

public interface VehicleModelRepository extends JpaRepository<VehicleModel, Long> {

}
