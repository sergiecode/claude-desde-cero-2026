package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.VehicleAgeFactor;

public interface VehicleAgeFactorRepository extends JpaRepository<VehicleAgeFactor, Long> {

}
