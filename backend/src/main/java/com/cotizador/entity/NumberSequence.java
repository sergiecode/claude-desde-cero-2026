package com.cotizador.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Yearly correlative counter per prefix ({@code COT}, {@code POL}); restarts at 1 every year. */
@Entity
@Table(name = "number_sequences")
@IdClass(NumberSequenceId.class)
public class NumberSequence {

	@Id
	@Column(name = "prefix", nullable = false, length = 5)
	private String prefix;

	@Id
	@Column(name = "year", nullable = false)
	private Integer year;

	@Column(name = "last_value", nullable = false)
	private Integer lastValue;

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer year) {
		this.year = year;
	}

	public Integer getLastValue() {
		return lastValue;
	}

	public void setLastValue(Integer lastValue) {
		this.lastValue = lastValue;
	}

}
