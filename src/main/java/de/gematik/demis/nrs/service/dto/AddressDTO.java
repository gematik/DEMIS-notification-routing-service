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

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Set;

public record AddressDTO(
    String street, String no, String postalCode, String city, String countryCode) {
  public static final String COUNTRY_CODE_GERMANY = "20422";
  public static final Set<String> GERMANY_CODES = Set.of(COUNTRY_CODE_GERMANY, "DE", "DEU");

  @JsonIgnore
  public boolean isGerman() {
    return countryCode() != null && GERMANY_CODES.contains(countryCode().toUpperCase());
  }
}
