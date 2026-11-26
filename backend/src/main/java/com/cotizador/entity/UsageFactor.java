package com.cotizador.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usage_factors")
public class UsageFactor {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "usage_type", nullable = false, length = 30)
	private VehicleUsageType usageType;

	@Column(name = "factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal factor;

	public VehicleUsageType getUsageType() {
		return usageType;
	}

	public void setUsageType(VehicleUsageType usageType) {
		this.usageType = usageType;
	}

	public BigDecimal getFactor() {
		return factor;
	}

	public void setFactor(BigDecimal factor) {
		this.factor = factor;
	}

}
