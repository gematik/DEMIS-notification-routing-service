package de.gematik.demis.nrs.service.dto;

/*-
 * #%L
 * notification-routing-service
 * %%
 * Copyright (C) 2025 gematik GmbH
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
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import java.util.InputMismatchException;
import java.util.Objects;
import java.util.Scanner;
import javax.annotation.Nonnull;

/**
 * A value object that represents health office ids. Many different writing styles exists and this
 * object aims to unify them to avoid errors due to typos and similar mistakes.
 */
public class HealthOfficeId {

  public static final HealthOfficeId RKI = new HealthOfficeId(1);

  private final int a;
  private final int b;
  private final int c;
  private final int d;
  private final int e;
  private String canonicalRepresentation;

  private HealthOfficeId(final int a) {
    this(a, 0, 0, 0, 0);
  }

  private HealthOfficeId(final int a, final int b, final int c, final int d, final int e) {
    this.a = a;
    this.b = b;
    this.c = c;
    this.d = d;
    this.e = e;
  }

  /** Format the given string as canonical String representation of the health office id. */
  @Nonnull
  public static String asCanonical(@Nonnull final String raw) {
    return HealthOfficeId.from(raw).getCanonicalRepresentation();
  }

  /** Attempt to parse the given string as health office id. */
  @Nonnull
  public static HealthOfficeId from(@Nonnull final String raw) {
    // Matching space here (\\s) helps us avoid having to call trim() etc.
    try (Scanner s = new Scanner(raw).useDelimiter("\\s*\\.\\s*")) {
      final int[] result;
      result = new int[5];
      int i = 0;
      while (s.hasNext()) {
        if (i == 5) {
          throw new IllegalArgumentException(
              String.format("Can't parse string '%s' as health office id: too many parts", raw));
        }
        try {
          final int part = s.nextInt();
          result[i] = part;
          i++;
        } catch (InputMismatchException e) {
          // Tried to parse int but got something else
          throw new IllegalArgumentException(
              String.format("Can't parse string '%s' as health office id: invalid format", raw), e);
        }
      }
      return HealthOfficeId.of(result[0], result[1], result[2], result[3], result[4]);
    }
  }

  @Nonnull
  public static HealthOfficeId of(final int a) {
    return HealthOfficeId.of(a, 0, 0, 0, 0);
  }

  @Nonnull
  public static HealthOfficeId of(final int a, final int b, final int c, final int d) {
    return HealthOfficeId.of(a, b, c, d, 0);
  }

  @Nonnull
  public static HealthOfficeId of(final int a, final int b, final int c, final int d, final int e) {
    if (a == 1 && b == 0 && c == 0 && d == 0 && e == 0) {
      return RKI;
    } else if (a > 1 && b == 0 && c == 0 && d == 0 && e == 0) {
      return new HealthOfficeId(a);
    }

    // 0.x.x.x.x is forbidden
    if (a == 0) {
      throw new IllegalArgumentException("First numeral MUST NOT be 0.");
    }

    if (b == 0) {
      throw new IllegalArgumentException("Second numeral MUST NOT be 0.");
    }

    if (c > 9) {
      throw new IllegalArgumentException("Third numeral MUST NOT be greater than 9.");
    }

    if (d == 0) {
      throw new IllegalArgumentException("Fourth numeral MUST NOT be 0.");
    }

    if (a < 0 || b < 0 || c < 0 || d < 0 || e < 0) {
      throw new IllegalArgumentException("Negative numbers are not allowed.");
    }

    return new HealthOfficeId(a, b, c, d, e);
  }

  /**
   * An id is MUST be made up of either 1, 4 or 5 numerals separated by a period('.') and terminated
   * by a period.
   *
   * <p>The first numeral MUST NOT start with leading zero or be zero.<br>
   * Correct: 1.2.3.4.5., 10.2.3.4.5. Incorrect: 01.2.3.4.5., 010.2.3.4.5.
   *
   * <p>The second numeral MUST consist of at least two digits. Zero is not permitted. Single digit
   * numerals MUST be padded with a single leading zero.<br>
   * Correct: 1.01.0.01., 1.11.2.54., 1.10.02.10.03. Incorrect: 1.1.03.1., 1.0.2.51.
   *
   * <p>The third numeral MUST consist of exactly one digit. Correct: 1.16.0.63., 1.01.0.03.,
   * 1.03.1.01. Incorrect: 1.16.00.63, 1.03.01.01.
   *
   * <p>The fourth numeral MUST consist of at least two digits. Zero is not permitted. Single digit
   * numerals MUST be padded with a single leading zero.<br>
   * Correct: 1.01.0.01., 1.11.2.54., 1.11.0.01.01. Incorrect: 1.01.0.1., 1.01.1.0.
   *
   * <p>The fifth numeral is optional. The numeral MUST consist of at least two digits. Zero is not
   * permitted. Single digit numerals MUST be padded with a single leading zero.<br>
   * Correct: 1.01.0.01., 1.11.2.54., 1.11.0.01.01. Incorrect: 1.01.0.1., 1.01.1.0.
   *
   * <p>The id MUST be terminated by a period.<br>
   * Correct: 1.2.3.4.5., 1. Incorrect: 1.2.3.4.5, 1
   *
   * <p>Single digit ids MUST NOT be filled up.<br>
   * Correct: 1. Incorrect: 1.0.0.0.0.
   */
  @Nonnull
  private static String computeCanonicalRepresentation(
      final int a, final int b, final int c, final int d, final int e) {
    if (a > 0 && b == 0 && c == 0 && d == 0 && e == 0) {
      return String.format("%d.", a);
    }

    if (e == 0) {
      return String.format("%d.%02d.%d.%02d.", a, b, c, d);
    } else {
      return String.format("%d.%02d.%d.%02d.%02d.", a, b, c, d, e);
    }
  }

  @Override
  public String toString() {
    return getCanonicalRepresentation();
  }

  @Nonnull
  public String getCanonicalRepresentation() {
    if (canonicalRepresentation == null) {
      canonicalRepresentation = computeCanonicalRepresentation(a, b, c, d, e);
    }
    return canonicalRepresentation;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    final HealthOfficeId that = (HealthOfficeId) o;
    return a == that.a && b == that.b && c == that.c && d == that.d && e == that.e;
  }

  @Override
  public int hashCode() {
    return Objects.hash(a, b, c, d, e);
  }
}
