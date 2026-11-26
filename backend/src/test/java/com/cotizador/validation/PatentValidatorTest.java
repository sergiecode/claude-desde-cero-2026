package com.cotizador.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.cotizador.exception.BusinessException;
import com.cotizador.exception.ErrorCode;
import com.cotizador.exception.FieldErrorResponse;

/** Patent normalization and format (rule V6, backend.md §5.1). */
class PatentValidatorTest {

	private final PatentValidator validator = new PatentValidator();

	@ParameterizedTest
	@CsvSource(value = {
			"ABC123|ABC123",
			"abc123|ABC123",
			"' abc123 '|ABC123",
			"aBc 123|ABC123",
			"'\tXYZ999\n'|XYZ999"
	}, delimiter = '|')
	void AC_VEH_11_V6_patentIsUppercasedWithoutSpaces(String patent, String expected) {
		assertThat(validator.normalize(patent)).isEqualTo(expected);
	}

	@ParameterizedTest
	@ValueSource(strings = { "AB123CD", "AB1234", "ABCD12", "ABC12", "123ABC", "ÑBC123", "ABC-123", "", "   " })
	void AC_VEH_10_V6_invalidFormatIsRejectedWithPatentFieldError(String patent) {
		assertInvalid(patent);
	}

	@ParameterizedTest
	@NullSource
	void V6_nullPatentIsInvalid(String patent) {
		assertInvalid(patent);
	}

	private void assertInvalid(String patent) {
		assertThatThrownBy(() -> validator.normalize(patent))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getCode()).isEqualTo(ErrorCode.INVALID_PATENT);
					assertThat(ex.getMessage()).isEqualTo("La patente debe tener el formato ABC123");
					assertThat(ex.getErrors()).containsExactly(
							new FieldErrorResponse("patent", "La patente debe tener el formato ABC123"));
				});
	}

}
