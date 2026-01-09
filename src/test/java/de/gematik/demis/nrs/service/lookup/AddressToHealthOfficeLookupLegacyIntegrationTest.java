package de.gematik.demis.nrs.service.lookup;

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

import static de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup.LookupStatus.DETAIL_NOT_PROVIDED;
import static de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup.LookupStatus.MULTIPLE_MATCHES;
import static de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup.LookupStatus.NOT_FOUND;
import static de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup.LookupStatus.UNIQUE_MATCH;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.CITY_STREET;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.FALLBACK_POSTALCODE;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.FALLBACK_POSTALCODE_CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE_CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE_CITY_STREET;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup.LookupStatus;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = NotificationRoutingApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup",
      "nrs.checkWorkaroundIsWorking=false"
    })
class AddressToHealthOfficeLookupLegacyIntegrationTest {

  @Autowired AddressToHealthOfficeLookup underTest;

  @Autowired MeterRegistry meterRegistry;

  static Stream<Arguments> testInput() {
    return Stream.of(
        Arguments.of(createAddress("44801", null, null, "FR"), null, Map.of()),
        Arguments.of(
            createAddress("44801", null, null, "DE"), "1.10", Map.of(POSTALCODE, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("44801", null, null, "DEU"), "1.10", Map.of(POSTALCODE, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("44801", null, null, "20422"), "1.10", Map.of(POSTALCODE, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("99999", null, null),
            null,
            Map.of(POSTALCODE, NOT_FOUND, CITY, DETAIL_NOT_PROVIDED)),
        Arguments.of(
            createAddress("13055", "Berlin", null),
            "3.14",
            Map.of(POSTALCODE, MULTIPLE_MATCHES, POSTALCODE_CITY, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("13055", "Berlin", "Wird-nicht-benutzt-Weg"),
            "3.14",
            Map.of(POSTALCODE, MULTIPLE_MATCHES, POSTALCODE_CITY, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("13055", "Potsdam", null),
            "3.15",
            Map.of(POSTALCODE, MULTIPLE_MATCHES, POSTALCODE_CITY, UNIQUE_MATCH)),
        Arguments.of(
            createAddress("13055", "Gibt-es-nicht", null),
            "3.16",
            Map.of(
                POSTALCODE,
                MULTIPLE_MATCHES,
                POSTALCODE_CITY,
                NOT_FOUND,
                FALLBACK_POSTALCODE,
                UNIQUE_MATCH)),
        Arguments.of(
            createAddress("21481", "BUCHHORST", "Krankenhausstr."),
            "5.6.7",
            Map.of(
                POSTALCODE,
                MULTIPLE_MATCHES,
                POSTALCODE_CITY,
                MULTIPLE_MATCHES,
                POSTALCODE_CITY_STREET,
                UNIQUE_MATCH)),
        Arguments.of(
            createAddress("21481", "BUCHHORST", "Weg"),
            "5.6.8",
            Map.of(
                POSTALCODE,
                MULTIPLE_MATCHES,
                POSTALCODE_CITY,
                MULTIPLE_MATCHES,
                POSTALCODE_CITY_STREET,
                NOT_FOUND,
                FALLBACK_POSTALCODE_CITY,
                UNIQUE_MATCH)),
        Arguments.of(
            createAddress("99999", "Bergisch Gladbach", "Egal"),
            "1.13",
            Map.of(POSTALCODE, NOT_FOUND, CITY, UNIQUE_MATCH)),
        Arguments.of(
            createAddress(null, "Bergisch Gladbach", null),
            "1.13",
            Map.of(POSTALCODE, DETAIL_NOT_PROVIDED, CITY, UNIQUE_MATCH)),
        Arguments.of(
            createAddress(null, "Unbekannt", "Egal"),
            null,
            Map.of(POSTALCODE, DETAIL_NOT_PROVIDED, CITY, NOT_FOUND)),
        Arguments.of(
            createAddress(null, "Köln", "Deutzerstr."),
            "9.1.3",
            Map.of(
                POSTALCODE,
                DETAIL_NOT_PROVIDED,
                CITY,
                MULTIPLE_MATCHES,
                CITY_STREET,
                UNIQUE_MATCH)),
        Arguments.of(
            createAddress(null, "Köln", "Heumarkt"),
            "9.1.4",
            Map.of(
                POSTALCODE,
                DETAIL_NOT_PROVIDED,
                CITY,
                MULTIPLE_MATCHES,
                CITY_STREET,
                UNIQUE_MATCH)),
        Arguments.of(
            createAddress(null, "Köln", "Ohne-Zustaendigkeit"),
            null,
            Map.of(
                POSTALCODE, DETAIL_NOT_PROVIDED, CITY, MULTIPLE_MATCHES, CITY_STREET, NOT_FOUND)));
  }

  private static AddressDTO createAddress(
      final String postalCode, final String city, final String street) {
    return createAddress(postalCode, city, street, AddressDTO.COUNTRY_CODE_GERMANY);
  }

  private static AddressDTO createAddress(
      final String postalCode, final String city, final String street, final String countryCode) {
    return new AddressDTO(street, null, postalCode, city, countryCode);
  }

  @BeforeEach
  void clearMeterRegistry() {
    meterRegistry.clear();
  }

  @ParameterizedTest
  @MethodSource("testInput")
  void lookup(
      final AddressDTO address,
      final String expected,
      final Map<LookupMap, LookupStatus> lookupCounters) {
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).isEqualTo(Optional.ofNullable(expected));
    assertStatistic(lookupCounters);
  }

  private void assertStatistic(final Map<LookupMap, LookupStatus> expectedLookupCounters) {
    final var expected =
        expectedLookupCounters.entrySet().stream()
            .collect(
                Collectors.toMap(
                    e -> Tags.of("map", e.getKey().name(), "status", e.getValue().name()),
                    e -> 1.0d));

    final Map<Tags, Double> counters =
        meterRegistry.find("address_csv_lookup").counters().stream()
            .collect(Collectors.toMap(c -> Tags.of(c.getId().getTags()), Counter::count));

    Assertions.assertThat(counters).isEqualTo(expected);
  }
}
