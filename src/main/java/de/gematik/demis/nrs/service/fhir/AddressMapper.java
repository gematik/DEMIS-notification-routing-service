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

import de.gematik.demis.nrs.service.dto.AddressDTO;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.StringType;
import org.springframework.stereotype.Service;

@Service
class AddressMapper {
  private static final Pattern STREET_NO_PATTERN =
      Pattern.compile("^(.*)\\s+(\\d+\\s*[\\./\\-0-9a-zA-Z]*)$");

  private static final String[] EMPTY_STREET = {null, null};

  public AddressDTO toDto(final Address a) {
    final String[] streetAndNumber = splitLine(a.getLine());
    return new AddressDTO(
        streetAndNumber[0], streetAndNumber[1], a.getPostalCode(), a.getCity(), a.getCountry());
  }

  private String[] splitLine(final List<StringType> lines) {
    if (lines.isEmpty()) {
      return EMPTY_STREET;
    } else {
      // Note: Profile ensure that we have only one line at most
      final String lineValue = lines.get(0).getValueAsString();
      final Matcher matcher = STREET_NO_PATTERN.matcher(lineValue);
      if (matcher.matches()) {
        return new String[] {matcher.group(1), matcher.group(2)};
      } else {
        return new String[] {lineValue, null}; // TODO Abweichung zu vorher -> checken
      }
    }
  }
}
