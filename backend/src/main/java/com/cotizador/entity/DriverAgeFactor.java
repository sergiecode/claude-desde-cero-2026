package com.cotizador.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Driver age range (inclusive) and its factor. {@code maxAge} null means no upper limit. */
@Entity
@Table(name = "driver_age_factors")
public class DriverAgeFactor {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "min_age", nullable = false)
	private Integer minAge;

	@Column(name = "max_age")
	private Integer maxAge;

	@Column(name = "factor", nullable = false, precision = 6, scale = 4)
	private BigDecimal factor;

	public Long getId() {
		return id;
	}

	public Integer getMinAge() {
		return minAge;
	}

	public void setMinAge(Integer minAge) {
		this.minAge = minAge;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	public BigDecimal getFactor() {
		return factor;
	}

	public void setFactor(BigDecimal factor) {
		this.factor = factor;
	}

}
