package de.gematik.demis.nrs.service.lookup;

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

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.gematik.demis.nrs.util.sis2.SIS2TransformationService;
import org.junit.jupiter.api.Test;

class LegacyAddressNormalizationTest {

  private final AddressNormalization underTest =
      new AddressNormalization(
          new SIS2TransformationService(), new TransliterationService(), false);

  @Test
  void normalizePostalCode() {
    final String postalCode = "12345";
    assertEquals(postalCode, underTest.normalizePostalCode(postalCode));
  }

  @Test
  void normalizeCity() {
    assertEquals("KOELN", underTest.normalizeCity("Köln"));
  }

  @Test
  void normalizeStreet() {
    assertEquals("MEINEERSTESTRASSE", underTest.normalizeStreet("Meine erste Str."));
  }

  @Test
  void normalizeStreetNoExt() {
    assertEquals("15A", underTest.normalizeStreetNoExt("    15a    "));
  }
}
