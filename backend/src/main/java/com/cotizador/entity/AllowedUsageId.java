package com.cotizador.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite primary key of {@link AllowedUsage}. */
public class AllowedUsageId implements Serializable {

	private VehicleType vehicleType;

	private VehicleUsageType usageType;

	public AllowedUsageId() {
	}

	public AllowedUsageId(VehicleType vehicleType, VehicleUsageType usageType) {
		this.vehicleType = vehicleType;
		this.usageType = usageType;
	}

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public VehicleUsageType getUsageType() {
		return usageType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof AllowedUsageId other)) {
			return false;
		}
		return vehicleType == other.vehicleType && usageType == other.usageType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(vehicleType, usageType);
	}

}
