package com.cotizador.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A vehicle version (table {@code versions}). */
@Entity
@Table(name = "versions")
public class VehicleVersion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "model_id", nullable = false)
	private VehicleModel model;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	/** 0 km reference value in ARS; null means no valuation (rule S6). */
	@Column(name = "reference_value", precision = 15, scale = 2)
	private BigDecimal referenceValue;

	public Long getId() {
		return id;
	}

	public VehicleModel getModel() {
		return model;
	}

	public void setModel(VehicleModel model) {
		this.model = model;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getReferenceValue() {
		return referenceValue;
	}

	public void setReferenceValue(BigDecimal referenceValue) {
		this.referenceValue = referenceValue;
	}

}
