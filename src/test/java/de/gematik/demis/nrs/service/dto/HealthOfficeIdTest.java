package de.gematik.demis.nrs.service.dto;

/*-
 * #%L
 * notification-routing-service
 * %%
 * Copyright (C) 2025 - 2026 gematik GmbH
 * %%
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the
 * European Commission – subsequent versions of the EUPL (the "Licence").
 * You may not use this work except in compliance with the Licence.
 *
 * You find a copy of the Licence in the "Licence" file or at
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 * In case of changes by gematik find details in the "Readme" file.
 *
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik,
 * find details in the "Readme" file.
 * #L%
 */

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HealthOfficeIdTest {

  @Test
  void thatItCanParseVariousStringFormats() {
    assertThat(HealthOfficeId.from("1.")).isEqualTo(HealthOfficeId.of(1));
    assertThat(HealthOfficeId.from("10.")).isEqualTo(HealthOfficeId.of(10));
    assertThat(HealthOfficeId.from("1.01.0.01.")).isEqualTo(HealthOfficeId.of(1, 1, 0, 1));
    assertThat(HealthOfficeId.from("1.01.0.57.")).isEqualTo(HealthOfficeId.of(1, 1, 0, 57));
    assertThat(HealthOfficeId.from("1.03.1.55.03.")).isEqualTo(HealthOfficeId.of(1, 3, 1, 55, 3));
    assertThat(HealthOfficeId.from("1.03.1.55.03.")).isEqualTo(HealthOfficeId.of(1, 3, 1, 55, 3));
    assertThat(HealthOfficeId.from("1.14.5.21.02.")).isEqualTo(HealthOfficeId.of(1, 14, 5, 21, 2));
    assertThat(HealthOfficeId.from("1.11.0.11.09.")).isEqualTo(HealthOfficeId.of(1, 11, 0, 11, 9));
  }

  @Test
  void thatFirstNumeralNeverStartsWithLeadingZero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(0))
        .withMessage("First numeral MUST NOT be 0.");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(0, 1, 1, 1))
        .withMessage("First numeral MUST NOT be 0.");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(0, 1, 1, 1, 1))
        .withMessage("First numeral MUST NOT be 0.");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from("0.1.1.1.1."))
        .withMessage("First numeral MUST NOT be 0.");
  }

  @Test
  void thatSecondNumeralCantBeZero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 0, 1, 1))
        .withMessage("Second numeral MUST NOT be 0.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 0, 1, 1, 1))
        .withMessage("Second numeral MUST NOT be 0.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from("1.0.1.1.1."))
        .withMessage("Second numeral MUST NOT be 0.");
  }

  @Test
  void thatThirdNumeralCantBeGreaterThanNine() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 1, 10, 1))
        .withMessage("Third numeral MUST NOT be greater than 9.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 1, 10, 1, 1))
        .withMessage("Third numeral MUST NOT be greater than 9.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from("1.1.10.1.1."))
        .withMessage("Third numeral MUST NOT be greater than 9.");

    assertThatNoException().isThrownBy(() -> HealthOfficeId.from("1.1.9.1.1."));
  }

  @Test
  void thatFourthNumeralCantBeZero() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 1, 1, 0))
        .withMessage("Fourth numeral MUST NOT be 0.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.of(1, 1, 1, 0, 1))
        .withMessage("Fourth numeral MUST NOT be 0.");
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from("1.1.1.0."))
        .withMessage("Fourth numeral MUST NOT be 0.");
  }

  @Test
  void thatEqualsIsCorrectlyImplemented() {
    final HealthOfficeId a = HealthOfficeId.from("1.");
    final HealthOfficeId b = HealthOfficeId.of(1);
    final HealthOfficeId c = HealthOfficeId.of(1, 0, 0, 0, 0);

    assertThat(a).isEqualTo(b);
    assertThat(b).isEqualTo(c);
    assertThat(a).isEqualTo(c);
  }

  @ValueSource(
      strings = {
        "1.2", "1.2.3",
      })
  @ParameterizedTest
  void throwsErrorForMissingValues(final String input) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from(input))
        .withMessage("Fourth numeral MUST NOT be 0.");
  }

  @ValueSource(
      strings = {
        "1 . 02 . 0 3 . 4 . 5 . ",
        "1..0.0.0.",
        "1.....",
        "1..",
        "1.....",
      })
  @ParameterizedTest
  void thatInvalidFormatsCauseExceptions(final String input) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from(input))
        .withMessage("Can't parse string '%s' as health office id: invalid format", input);
  }

  @Test
  void throwsErrorForTooManyZeros() {
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(0, 1, 1, 1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, 1, 0, 0, 0));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, 1, 1, 0, 0));
  }

  @Test
  void thatNegativeNumbersAreNotAllowed() {
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(-1, 1, 1, 1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, -1, 1, 1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, 1, -1, 1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, 1, 1, -1, 1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.of(1, 1, 1, -1, -1));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.from("-1"));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.from("1. - 1.1.1."));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.from("1.1.-1.1."));
    assertThatIllegalArgumentException().isThrownBy(() -> HealthOfficeId.from("1.1.1.-1."));
  }

  @ValueSource(strings = {"1.2.3.4.5.6", "1.2.3.4.5.6.", "1.2.3.4.5.."})
  @ParameterizedTest
  void throwsErrorForOverflow(final String input) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> HealthOfficeId.from(input))
        .withMessage("Can't parse string '%s' as health office id: too many parts", input);
  }

  @Test
  void thatCanonicalRepresentationIsCorrectlyImplemented() {
    assertThat(HealthOfficeId.RKI.getCanonicalRepresentation()).isEqualTo("1.");

    // Pad second numeral with leading 0
    HealthOfficeId healthOfficeId = HealthOfficeId.of(1, 9, 7, 77);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.09.7.77.");

    healthOfficeId = HealthOfficeId.of(1, 10, 7, 77);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.10.7.77.");

    // Pad fourth numeral with leading 0
    healthOfficeId = HealthOfficeId.of(1, 3, 4, 1);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.03.4.01.");

    // Fifth is optional
    healthOfficeId = HealthOfficeId.of(1, 0, 0, 0, 0);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.");
    healthOfficeId = HealthOfficeId.of(10, 0, 0, 0, 0);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("10.");
    healthOfficeId = HealthOfficeId.of(1, 3, 2, 57, 1);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.03.2.57.01.");
    healthOfficeId = HealthOfficeId.of(1, 3, 2, 57, 0);
    assertThat(healthOfficeId.getCanonicalRepresentation()).isEqualTo("1.03.2.57.");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"1.03.2.57.01.", "1.03.2.57.01", "1.3.2.57.01.", "1.3.2.57.1.", "1.03.02.57.01."})
  void testAsCanonicalCanNormalizeInput(final String input) {
    String canonical = HealthOfficeId.asCanonical(input);
    assertThat(canonical).isEqualTo("1.03.2.57.01.");
  }
}
