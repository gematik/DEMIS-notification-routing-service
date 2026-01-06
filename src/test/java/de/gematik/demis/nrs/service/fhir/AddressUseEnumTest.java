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

import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Address;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AddressUseEnumTest {

  static Stream<Arguments> addresses() {
    return Stream.of(
        Arguments.of("primary", AddressUseEnum.PRIMARY),
        Arguments.of("current", AddressUseEnum.CURRENT),
        Arguments.of("ordinary", AddressUseEnum.ORDINARY),
        Arguments.of("some_value", AddressUseEnum.OTHER),
        Arguments.of("", AddressUseEnum.OTHER));
  }

  @ParameterizedTest
  @MethodSource(value = "addresses")
  void fromAddress(final String code, final AddressUseEnum expected) {
    final Address address = new AddressDataBuilder().withAddressUseExtension(code, null).build();
    final AddressUseEnum result = AddressUseEnum.fromAddress(address);
    Assertions.assertEquals(expected, result);
  }

  void fromNullAddress() {
    final AddressUseEnum result = AddressUseEnum.fromAddress(null);
    Assertions.assertEquals(AddressUseEnum.OTHER, result);
  }
}
