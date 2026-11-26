package com.cotizador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** A vehicle model (table {@code models}). Available for a year if {@code availableFromYear <= year}. */
@Entity
@Table(name = "models")
public class VehicleModel {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "brand_id", nullable = false)
	private Brand brand;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "available_from_year", nullable = false)
	private Integer availableFromYear;

	public Long getId() {
		return id;
	}

	public Brand getBrand() {
		return brand;
	}

	public void setBrand(Brand brand) {
		this.brand = brand;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getAvailableFromYear() {
		return availableFromYear;
	}

	public void setAvailableFromYear(Integer availableFromYear) {
		this.availableFromYear = availableFromYear;
	}

}
