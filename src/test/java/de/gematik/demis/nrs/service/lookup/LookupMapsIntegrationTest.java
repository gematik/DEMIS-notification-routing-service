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
import static org.junit.jupiter.api.Assertions.assertNull;

import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(
    classes = LookupMapsIntegrationTest.TestConfig.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "nrs.lookup-data-directory=src/test/resources/lookup/maps")
class LookupMapsIntegrationTest {

  @Autowired LookupMaps lookupMaps;

  @Test
  void test() {
    assertEquals("1.2", lookupMaps.getValue(LookupMap.CITY, new String[] {"BOCHUM"}));
    assertEquals(
        "1.2.3", lookupMaps.getValue(LookupMap.CITY_STREET, new String[] {"BOCHUM", "TESTSTR"}));
    assertEquals(
        "1.2.3.4",
        lookupMaps.getValue(LookupMap.CITY_STREET_NO, new String[] {"BOCHUM", "TESTSTR", "15b"}));
    assertEquals(
        "9",
        lookupMaps.getValue(
            LookupMap.POSTALCODE,
            new String[] {
              "10115",
            }));
    assertEquals(
        "9.2", lookupMaps.getValue(LookupMap.POSTALCODE_CITY, new String[] {"10115", "BERLIN"}));
    assertEquals(
        "9.2.3",
        lookupMaps.getValue(
            LookupMap.POSTALCODE_CITY_STREET, new String[] {"10115", "BERLIN", "FRIEDRICHSTR"}));
    assertEquals(
        "9.2.3.4",
        lookupMaps.getValue(
            LookupMap.POSTALCODE_CITY_STREET_NO,
            new String[] {"10115", "BERLIN", "FRIEDRICHSTR", "2"}));
    assertEquals("5.2", lookupMaps.getValue(LookupMap.FALLBACK_POSTALCODE, new String[] {"51427"}));
    assertEquals(
        "5.3",
        lookupMaps.getValue(LookupMap.FALLBACK_POSTALCODE_CITY, new String[] {"51427", "KOELN"}));
    assertEquals(
        "5.4",
        lookupMaps.getValue(
            LookupMap.FALLBACK_POSTALCODE_CITY_STREET, new String[] {"51427", "KOELN", "WEG"}));

    for (final LookupMap map : LookupMap.values()) {
      assertNull(lookupMaps.getValue(map, new String[] {"KEY_NOT_EXISTS"}), map.name());
    }
  }

  @Configuration
  @EnableConfigurationProperties(NrsConfigProps.class)
  @Import({CsvReader.class, LookupMapsLoader.class})
  static class TestConfig {}
}
