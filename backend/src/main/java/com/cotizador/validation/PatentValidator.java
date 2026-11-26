package com.cotizador.validation;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;

/** Normalizes and validates the patent (rule V6, backend.md §5.1). */
@Component
public class PatentValidator {

	static final String FIELD = "patent";

	private static final Pattern PATENT_PATTERN = Pattern.compile("^[A-Z]{3}\\d{3}$");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	/**
	 * Returns the patent in uppercase and without spaces, as it is stored.
	 *
	 * @throws BusinessException with {@link ErrorCode#INVALID_PATENT} (field {@code patent}) if the normalized
	 *         value is not three letters followed by three digits
	 */
	public String normalize(String patent) {
		if (patent == null) {
			throw invalid();
		}
		String normalized = WHITESPACE.matcher(patent).replaceAll("").toUpperCase(Locale.ROOT);
		if (!PATENT_PATTERN.matcher(normalized).matches()) {
			throw invalid();
		}
		return normalized;
	}

	private static BusinessException invalid() {
		return BusinessException.forField(ErrorCode.INVALID_PATENT, FIELD);
	}

}
