package com.cotizador.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Vehicle age range (inclusive) and its factor. {@code maxYears} null means no upper limit. */
@Entity
@Table(name = "vehicle_age_factors")
public class VehicleAgeFactor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "min_years", nullable = false)
	private Integer minYears;

	@Column(name = "max_years")
	private Integer maxYears;

	@Column(name = "factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal factor;

	public Long getId() {
		return id;
	}

	public Integer getMinYears() {
		return minYears;
	}

	public void setMinYears(Integer minYears) {
		this.minYears = minYears;
	}

	public Integer getMaxYears() {
		return maxYears;
	}

	public void setMaxYears(Integer maxYears) {
		this.maxYears = maxYears;
	}

	public BigDecimal getFactor() {
		return factor;
	}

	public void setFactor(BigDecimal factor) {
		this.factor = factor;
	}

}
