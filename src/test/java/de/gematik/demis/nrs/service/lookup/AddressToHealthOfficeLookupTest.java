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

import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.CITY_STREET;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.CITY_STREET_NO;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.FALLBACK_POSTALCODE;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.FALLBACK_POSTALCODE_CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.FALLBACK_POSTALCODE_CITY_STREET;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE_CITY;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE_CITY_STREET;
import static de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap.POSTALCODE_CITY_STREET_NO;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.service.Statistics;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.AdditionalAnswers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddressToHealthOfficeLookupTest {

  private static final String MANY = "many";
  private static final String HEALTH_OFFICE = "1.12";
  private static final AddressDTO GERMAN_ADDRESS = new AddressBuilder().build();

  @Mock LookupMaps lookupMaps;
  @Mock AddressNormalization addressNormalization;
  @Mock Statistics statistics;

  @InjectMocks AddressToHealthOfficeLookup underTest;

  private static Stream<Arguments> matchParams() {
    return Stream.of(
        // iterative lookup postalCode based
        Arguments.of(Map.of(POSTALCODE, LookupResultType.MATCH)),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET_NO, LookupResultType.MATCH))),

        // Fallbacks

        // POSTCODE -> iterative City Based Lookup
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET_NO, LookupResultType.MATCH))),

        // Fallback Maps
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE_CITY, LookupResultType.MATCH))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET_NO, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE_CITY_STREET, LookupResultType.MATCH))));
  }

  private static Stream<Arguments> notFoundParams() {
    return Stream.of(
        // City based
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.NOT_FOUND))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET, LookupResultType.NOT_FOUND))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_FOUND),
                entry(CITY, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET, LookupResultType.NOT_UNIQUE),
                entry(CITY_STREET_NO, LookupResultType.NOT_FOUND))),

        // Fallback Maps
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE, LookupResultType.NOT_FOUND))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE_CITY, LookupResultType.NOT_FOUND))),
        Arguments.of(
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET, LookupResultType.NOT_UNIQUE),
                entry(POSTALCODE_CITY_STREET_NO, LookupResultType.NOT_FOUND),
                entry(FALLBACK_POSTALCODE_CITY_STREET, LookupResultType.NOT_FOUND))));
  }

  private static Stream<Arguments> addressDetailsNotProvidedParams() {
    return Stream.of(
        Arguments.of(
            new AddressBuilder().setPostalCode(null).build(),
            Map.of(CITY, LookupResultType.MATCH),
            true),
        Arguments.of(
            new AddressBuilder().setPostalCode(null).setCity(null).build(), Map.of(), false),
        Arguments.of(
            new AddressBuilder().setCity("").build(),
            Map.ofEntries(
                entry(POSTALCODE, LookupResultType.NOT_UNIQUE),
                entry(FALLBACK_POSTALCODE, LookupResultType.MATCH)),
            true));
  }

  @BeforeEach
  void setupAddressNormalization() {
    when(addressNormalization.normalizePostalCode(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeCity(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeStreet(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeStreetNoExt(anyString())).then(returnsFirstArg());
  }

  @ParameterizedTest
  @ValueSource(strings = {"020422", "20421", "2042", "D", "DEUT", "DEUTSCHLAND", "FR"})
  @NullAndEmptySource
  void notGerman(final String countryCode) {
    final AddressDTO address = new AddressBuilder().setCountryCode(countryCode).build();
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).isNotPresent();
    Mockito.verifyNoInteractions(lookupMaps, addressNormalization);
    Mockito.verify(statistics).incNotGermanAddress(StringUtils.isNotBlank(countryCode));
  }

  @ParameterizedTest
  @ValueSource(strings = {"20422", "DE", "de", "DEU", "deu", "dEu"})
  void german(final String countryCode) {
    final String postalCode = "12345";
    final AddressDTO address =
        new AddressBuilder().setCountryCode(countryCode).setPostalCode(postalCode).build();
    setupLookupMaps(Map.of(POSTALCODE, LookupResultType.MATCH), address);
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).hasValue(HEALTH_OFFICE);
    Mockito.verify(statistics, Mockito.never()).incNotGermanAddress(any(Boolean.class));
  }

  @ParameterizedTest
  @MethodSource(value = "matchParams")
  void match(final Map<LookupMap, LookupResultType> lookupValues) {
    final AddressDTO address = GERMAN_ADDRESS;
    setupLookupMaps(lookupValues, address);
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).hasValue(HEALTH_OFFICE);
  }

  @ParameterizedTest
  @MethodSource(value = "notFoundParams")
  void notFound(final Map<LookupMap, LookupResultType> lookupValues) {
    final AddressDTO address = GERMAN_ADDRESS;
    setupLookupMaps(lookupValues, address);
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).isNotPresent();
  }

  @ParameterizedTest
  @MethodSource(value = "addressDetailsNotProvidedParams")
  void addressDetailsNotProvided(
      final AddressDTO address,
      final Map<LookupMap, LookupResultType> lookupValues,
      final boolean found) {
    setupLookupMaps(lookupValues, address);
    final Optional<String> result = underTest.lookup(address);

    assertThat(result).isEqualTo(Optional.ofNullable(found ? HEALTH_OFFICE : null));
  }

  private String[] buildKey(final LookupMap mapType, final AddressDTO address) {
    final var allKeyParts =
        Arrays.asList(address.postalCode(), address.city(), address.street(), address.no());
    final var result =
        switch (mapType) {
          case POSTALCODE, FALLBACK_POSTALCODE -> allKeyParts.subList(0, 1);
          case POSTALCODE_CITY, FALLBACK_POSTALCODE_CITY -> allKeyParts.subList(0, 2);
          case POSTALCODE_CITY_STREET, FALLBACK_POSTALCODE_CITY_STREET -> allKeyParts.subList(0, 3);
          case POSTALCODE_CITY_STREET_NO -> allKeyParts.subList(0, 4);
          case CITY -> allKeyParts.subList(1, 2);
          case CITY_STREET -> allKeyParts.subList(1, 3);
          case CITY_STREET_NO -> allKeyParts.subList(1, 4);
        };
    return result.toArray(new String[0]);
  }

  private void setupLookupMaps(
      final Map<LookupMap, LookupResultType> lookupValues, final AddressDTO address) {
    final List<LookupCall> expectedCalls =
        lookupValues.entrySet().stream()
            .map(
                entry ->
                    new LookupCall(
                        entry.getKey(),
                        buildKey(entry.getKey(), address),
                        entry.getValue().getValue()))
            .toList();
    setupLookupMaps(expectedCalls);
  }

  private void setupLookupMaps(final List<LookupCall> calls) {
    when(lookupMaps.getValue(any(LookupMap.class), any(String[].class)))
        .then(
            AdditionalAnswers.answer(
                (LookupMap map, String[] key) ->
                    calls.stream()
                        .filter(call -> call.map() == map && Arrays.equals(call.key(), key))
                        .map(call -> Optional.ofNullable(call.value()))
                        .findFirst()
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Unexpected Lookup Call " + map + ", " + Arrays.toString(key)))
                        .orElse(null)));
  }

  @Getter
  @RequiredArgsConstructor
  private enum LookupResultType {
    MATCH(HEALTH_OFFICE),
    NOT_FOUND(null),
    NOT_UNIQUE(MANY);
    private final String value;
  }

  private record LookupCall(LookupMap map, String[] key, String value) {}

  @Setter
  @Accessors(chain = true)
  private static class AddressBuilder {
    private String postalCode = "12345";
    private String city = "bochum";
    private String street = "testStr.";
    private String no = "15b";
    private String countryCode = "20422";

    public AddressDTO build() {
      return new AddressDTO(street, no, postalCode, city, countryCode);
    }
  }
}
