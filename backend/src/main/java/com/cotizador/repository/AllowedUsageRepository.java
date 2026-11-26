package com.cotizador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cotizador.entity.AllowedUsage;
import com.cotizador.entity.AllowedUsageId;
import com.cotizador.entity.VehicleType;

public interface AllowedUsageRepository extends JpaRepository<AllowedUsage, AllowedUsageId> {

	List<AllowedUsage> findByVehicleType(VehicleType vehicleType);

}
