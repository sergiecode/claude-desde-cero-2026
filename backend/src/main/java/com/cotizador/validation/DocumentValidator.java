package com.cotizador.validation;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.cotizador.entity.DocumentType;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;

/**
 * Normalizes and validates DNI and CUIT numbers (rules C3 and C4, backend.md §5.1 and §5.2).
 */
@Component
public class DocumentValidator {

	private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{7,8}$");
	private static final Pattern CUIT_PATTERN = Pattern.compile("^\\d{11}$");
	private static final int[] CUIT_WEIGHTS = { 5, 4, 3, 2, 7, 6, 5, 4, 3, 2 };

	/**
	 * Returns the document number as stored in the database (digits only; CUIT without dashes).
	 *
	 * @throws BusinessException with {@link ErrorCode#INVALID_DOCUMENT} if the number is not valid for the type
	 */
	public String normalize(DocumentType documentType, String documentNumber) {
		if (documentType == null || documentNumber == null) {
			throw invalid();
		}
		return switch (documentType) {
			case DNI -> normalizeDni(documentNumber);
			case CUIT -> normalizeCuit(documentNumber);
		};
	}

	private String normalizeDni(String documentNumber) {
		if (!DNI_PATTERN.matcher(documentNumber).matches()) {
			throw invalid();
		}
		return documentNumber;
	}

	private String normalizeCuit(String documentNumber) {
		String digits = documentNumber.replace("-", "");
		if (!CUIT_PATTERN.matcher(digits).matches() || !hasValidCheckDigit(digits)) {
			throw invalid();
		}
		return digits;
	}

	/** Check digit algorithm of backend.md §5.2. Expects exactly 11 digits. */
	private static boolean hasValidCheckDigit(String cuit) {
		int sum = 0;
		for (int i = 0; i < CUIT_WEIGHTS.length; i++) {
			sum += Character.digit(cuit.charAt(i), 10) * CUIT_WEIGHTS[i];
		}
		int remainder = 11 - (sum % 11);
		if (remainder == 10) {
			return false;
		}
		int expected = remainder == 11 ? 0 : remainder;
		return Character.digit(cuit.charAt(10), 10) == expected;
	}

	private static BusinessException invalid() {
		return new BusinessException(ErrorCode.INVALID_DOCUMENT);
	}

}
