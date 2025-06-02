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

import com.google.common.base.Strings;
import de.gematik.demis.nrs.service.lookup.LookupTree;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/** An immutable value object that holds address data to store in a {@link LookupTree}. */
public class LookupAddress {
  @Nonnull private final String healthOffice;
  @Nonnull private final String postalCode;
  @CheckForNull private final String city;
  @CheckForNull private final String street;
  @CheckForNull private final String number;
  @CheckForNull private final String extension;

  /**
   * Create a new instance.
   *
   * @param healthOffice The health office represented by this address
   * @param rest a var-arg referencing postalCode, city, street, number and extension. Additional
   *     arguments are ignored.
   */
  public LookupAddress(@Nonnull final String healthOffice, @Nonnull String... rest) {
    this.healthOffice = healthOffice;
    // first: we verify that the caller provided the required postal code
    Objects.checkIndex(0, rest.length);
    Objects.requireNonNull(
        Strings.emptyToNull(rest[0]), "Must provide non-empty value for postal code");
    this.postalCode = rest[0];
    // second: to avoid having to check if a value is present at the index, we simply extend the
    // original and fill up with null
    rest = Arrays.copyOf(rest, 5);
    this.city = Strings.emptyToNull(rest[1]);
    this.street = Strings.emptyToNull(rest[2]);
    this.number = Strings.emptyToNull(rest[3]);
    this.extension = Strings.emptyToNull(rest[4]);
  }

  @Override
  public String toString() {
    return "LookupAddress{"
        + "ga='"
        + healthOffice
        + '\''
        + ", postalCode='"
        + postalCode
        + '\''
        + ", city='"
        + city
        + '\''
        + ", street='"
        + street
        + '\''
        + ", number='"
        + number
        + '\''
        + ", extension='"
        + extension
        + '\''
        + '}';
  }

  @Nonnull
  public String getHealthOffice() {
    return healthOffice;
  }

  @Nonnull
  public String getPostalCode() {
    return postalCode;
  }

  @Nonnull
  public Optional<String> getCity() {
    return Optional.ofNullable(city);
  }

  @Nonnull
  public Optional<String> getStreet() {
    return Optional.ofNullable(street);
  }

  @Nonnull
  public Optional<String> getNumber() {
    return Optional.ofNullable(number);
  }

  @Nonnull
  public Optional<String> getExtension() {
    return Optional.ofNullable(extension);
  }
}
