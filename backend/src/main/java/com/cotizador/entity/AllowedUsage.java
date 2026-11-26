package com.cotizador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** An allowed (vehicle type, usage type) combination. Only allowed combinations have a row. */
@Entity
@Table(name = "allowed_usages")
@IdClass(AllowedUsageId.class)
public class AllowedUsage {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "vehicle_type", nullable = false, length = 20)
	private VehicleType vehicleType;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "usage_type", nullable = false, length = 30)
	private VehicleUsageType usageType;

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public VehicleUsageType getUsageType() {
		return usageType;
	}

	public void setUsageType(VehicleUsageType usageType) {
		this.usageType = usageType;
	}

}
