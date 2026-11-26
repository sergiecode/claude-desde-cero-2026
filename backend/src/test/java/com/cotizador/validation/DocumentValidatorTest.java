package com.cotizador.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.cotizador.entity.DocumentType;
import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;

/** Document format rules C3 and C4 (backend.md §5.1 and §5.2). */
class DocumentValidatorTest {

	private final DocumentValidator validator = new DocumentValidator();

	@Test
	void AC_CLI_04_dniWithThreeDigitsIsInvalid() {
		assertInvalid(DocumentType.DNI, "123");
	}

	@Test
	void AC_CLI_05_cuitWithWrongCheckDigitIsInvalid() {
		assertInvalid(DocumentType.CUIT, "30712345672");
	}

	@ParameterizedTest
	@ValueSource(strings = { "1234567", "12345678", "30123456", "8765432" })
	void C3_dniWithSevenOrEightDigitsIsValid(String dni) {
		assertThat(validator.normalize(DocumentType.DNI, dni)).isEqualTo(dni);
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "123456", "123456789", "3012345A", "30.123.456", "30-123456", " 30123456" })
	void C3_dniThatIsNotSevenOrEightDigitsIsInvalid(String dni) {
		assertInvalid(DocumentType.DNI, dni);
	}

	@ParameterizedTest
	@ValueSource(strings = { "30712345671", "30709876542" })
	void C4_cuitWithValidCheckDigitIsValid(String cuit) {
		assertThat(validator.normalize(DocumentType.CUIT, cuit)).isEqualTo(cuit);
	}

	@Test
	void AC_CLI_02_cuitWithDashesIsNormalizedToDigits() {
		assertThat(validator.normalize(DocumentType.CUIT, "30-71234567-1")).isEqualTo("30712345671");
	}

	@Test
	void C4_cuitWhoseRemainderIsElevenHasCheckDigitZero() {
		// 2×5 + 6×2 = 22; 22 mod 11 = 0; 11 − 0 = 11 → check digit 0
		assertThat(validator.normalize(DocumentType.CUIT, "20000000060")).isEqualTo("20000000060");
		assertInvalid(DocumentType.CUIT, "20000000061");
	}

	@Test
	void C4_cuitWhoseRemainderIsTenIsAlwaysInvalid() {
		// 2×5 + 1×2 = 12; 12 mod 11 = 1; 11 − 1 = 10 → invalid whatever the last digit
		for (int lastDigit = 0; lastDigit <= 9; lastDigit++) {
			assertInvalid(DocumentType.CUIT, "2000000001" + lastDigit);
		}
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "3071234567", "307123456711", "30-71234567", "3071234567A", "30 71234567 1" })
	void C4_cuitThatIsNotElevenDigitsIsInvalid(String cuit) {
		assertInvalid(DocumentType.CUIT, cuit);
	}

	@Test
	void C2_dniNumberIsNotAcceptedAsCuit() {
		assertInvalid(DocumentType.CUIT, "30123456");
	}

	@Test
	void nullValuesAreInvalid() {
		assertInvalid(DocumentType.DNI, null);
		assertInvalid(null, "30123456");
	}

	private void assertInvalid(DocumentType type, String number) {
		assertThatThrownBy(() -> validator.normalize(type, number))
				.isInstanceOf(BusinessException.class)
				.satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(ErrorCode.INVALID_DOCUMENT))
				.hasMessage("El número de documento no es válido");
	}

}
