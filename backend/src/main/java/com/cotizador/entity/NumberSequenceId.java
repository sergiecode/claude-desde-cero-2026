package com.cotizador.entity;

import java.io.Serializable;
import java.util.Objects;

/** Composite primary key of {@link NumberSequence}. */
public class NumberSequenceId implements Serializable {

	private String prefix;

	private Integer year;

	public NumberSequenceId() {
	}

	public NumberSequenceId(String prefix, Integer year) {
		this.prefix = prefix;
		this.year = year;
	}

	public String getPrefix() {
		return prefix;
	}

	public Integer getYear() {
		return year;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof NumberSequenceId other)) {
			return false;
		}
		return Objects.equals(prefix, other.prefix) && Objects.equals(year, other.year);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prefix, year);
	}

}
