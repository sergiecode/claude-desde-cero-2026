package com.cotizador.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.UsageFactor;
import com.cotizador.entity.VehicleUsageType;

public interface UsageFactorRepository extends JpaRepository<UsageFactor, VehicleUsageType> {

}
