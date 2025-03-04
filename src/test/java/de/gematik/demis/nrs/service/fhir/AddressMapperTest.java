package de.gematik.demis.nrs.service.fhir;

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
 * #L%
 */

import de.gematik.demis.nrs.service.dto.AddressDTO;
import org.assertj.core.api.Assertions;
import org.hl7.fhir.r4.model.Address;
import org.junit.jupiter.api.Test;

class AddressMapperTest {

  private final AddressMapper underTest = new AddressMapper();

  @Test
  void toDto() {
    final Address address =
        new Address()
            .setCountry("DEU")
            .setPostalCode("12345")
            .setCity("berlin")
            .addLine("Teststr. 15b");

    final AddressDTO dto = underTest.toDto(address);
    Assertions.assertThat(dto)
        .isEqualTo(new AddressDTO("Teststr.", "15b", "12345", "berlin", "DEU"));
  }

  @Test
  void splitLine_NoHouseNumber() {
    final Address address = new Address().addLine("Meinestr.");
    final AddressDTO dto = underTest.toDto(address);
    Assertions.assertThat(dto).isEqualTo(new AddressDTO("Meinestr.", null, null, null, null));
  }

  @Test
  void emptyAddress() {
    final AddressDTO dto = underTest.toDto(new Address());
    Assertions.assertThat(dto).isEqualTo(new AddressDTO(null, null, null, null, null));
  }
}
