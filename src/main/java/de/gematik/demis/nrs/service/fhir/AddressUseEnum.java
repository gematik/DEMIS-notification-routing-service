package de.gematik.demis.nrs.service.fhir;

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

import static de.gematik.demis.nrs.service.fhir.DemisFhirConstants.ADDRESS_USE_EXTENSION;
import static de.gematik.demis.nrs.service.fhir.DemisFhirConstants.ADDRESS_USE_SYSTEM;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;

@Getter
@RequiredArgsConstructor
enum AddressUseEnum {
  CURRENT("current"),
  ORDINARY("ordinary"),
  PRIMARY("primary"),
  ASKU("ASKU"), // TODO checken
  OTHER("");

  private static final Map<String, AddressUseEnum> CODE_TO_ENUM =
      Arrays.stream(values())
          .collect(Collectors.toMap(AddressUseEnum::getCode, Function.identity()));

  private final String code;

  /**
   * Returns the {@link AddressUseEnum} that's {@link #code} attribute equals the given {@code code}
   * parameter. If the {@code code} parameter does not match any {@code AddressUseEnum}'s code,
   * {@link #OTHER} will be returned.
   *
   * @param code the code of the AddressUseEnum to be returned
   * @return the AddressUseEnum, that's code attribute matches parameter {@code code}, or {@link
   *     #OTHER}, if none matches. Never null.
   */
  public static AddressUseEnum fromCode(final String code) {
    return CODE_TO_ENUM.getOrDefault(code, OTHER);
  }

  /**
   * Detects which address use is associated with the given {@code address} and returns the
   * indicator. This function will always return a value, if no known address use is found, this
   * function returns {@link AddressUseEnum#OTHER}.
   *
   * @param address the address to be checked for address use
   * @return the address use determined for {@code address}
   */
  public static AddressUseEnum fromAddress(final Address address) {
    if (address != null) {
      final Extension extension = address.getExtensionByUrl(ADDRESS_USE_EXTENSION);
      if (extension != null
          && extension.getValue() instanceof Coding coding
          && (ADDRESS_USE_SYSTEM.equals(coding.getSystem()))) {
        return fromCode(coding.getCode());
      }
    }
    return OTHER;
  }
}
