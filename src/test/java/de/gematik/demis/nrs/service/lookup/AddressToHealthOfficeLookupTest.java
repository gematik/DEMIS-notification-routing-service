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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.service.Statistics;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import java.util.Optional;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddressToHealthOfficeLookupTest {

  private static final String HEALTH_OFFICE = "1.12";
  private static final AddressDTO GERMAN_ADDRESS = new AddressBuilder().build();

  @Mock LookupMaps lookupMaps;
  @Mock LookupTree lookupTree;
  @Mock AddressNormalization addressNormalization;
  @Mock Statistics statistics;

  AddressToHealthOfficeLookup underTest;

  @BeforeEach
  void setup() {
    underTest =
        new AddressToHealthOfficeLookup(
            lookupMaps, addressNormalization, lookupTree, statistics, true, true);
    when(addressNormalization.normalizePostalCode(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeCity(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeStreet(anyString())).then(returnsFirstArg());
    when(addressNormalization.normalizeStreetNoExt(anyString())).then(returnsFirstArg());
  }

  @ParameterizedTest
  @ValueSource(strings = {"020422", "20421", "2042", "D", "DEUT", "DEUTSCHLAND", "FR"})
  @NullAndEmptySource
  void thatAddressesWithoutGermanCountryCodeAreNotLookedUp(final String countryCode) {
    final AddressDTO address = new AddressBuilder().setCountryCode(countryCode).build();
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).isNotPresent();
    Mockito.verifyNoInteractions(lookupMaps, addressNormalization);
    Mockito.verify(statistics).incNotGermanAddress(StringUtils.isNotBlank(countryCode));
  }

  @ParameterizedTest
  @ValueSource(strings = {"20422", "DE", "de", "DEU", "deu", "dEu"})
  void thatAddressesWithGermanCountryCodeAreLookedUp(final String countryCode) {
    final String postalCode = "12345";
    final AddressDTO address =
        new AddressBuilder().setCountryCode(countryCode).setPostalCode(postalCode).build();
    final LookupTree.LookupRequest from = LookupTree.LookupRequest.from(address);
    when(lookupTree.lookupHealthOffice(from))
        .thenReturn(
            Optional.of(
                new LookupTree.LookupResult(HEALTH_OFFICE, 0, LookupTree.Level.POSTAL_CODE)));
    when(addressNormalization.normalize(from)).thenReturn(from);
    final Optional<String> result = underTest.lookup(address);
    assertThat(result).hasValue(HEALTH_OFFICE);
    Mockito.verify(statistics, Mockito.never()).incNotGermanAddress(any(Boolean.class));
  }

  @Test
  void thatNotFoundReturnsEmptyResult() {
    final Optional<String> result = underTest.lookup(GERMAN_ADDRESS);
    assertThat(result).isNotPresent();
  }

  /**
   * Tests that are only relevant while we have the legacy and fuzzy implementation running in
   * parallel. Remove with the feature toggle.
   */
  @Nested
  class AlgorithmComparison {
    @Test
    void thatEqualResultsAreRecognized() {
      when(lookupTree.lookupHealthOffice(any())).thenReturn(Optional.empty());
      underTest.lookup(GERMAN_ADDRESS);
      verify(statistics)
          .recordLookupComparison(true, Statistics.LOOKUP_COMPARISON_RESULT_IDENTICAL);
    }

    @Test
    void thatDifferingResultsAreRecognized() {
      when(lookupTree.lookupHealthOffice(any()))
          .thenReturn(
              Optional.of(new LookupTree.LookupResult("any", 0, LookupTree.Level.POSTAL_CODE)));
      when(lookupMaps.getValue(any(), any())).thenReturn("other");
      underTest.lookup(GERMAN_ADDRESS);
      verify(statistics)
          .recordLookupComparison(false, Statistics.LOOKUP_COMPARISON_RESULT_BOTH_DIFFER);
    }

    @Test
    void thatFuzzyMatchIsRecognized() {
      when(lookupTree.lookupHealthOffice(any()))
          .thenReturn(
              Optional.of(
                  new LookupTree.LookupResult(HEALTH_OFFICE, 0, LookupTree.Level.POSTAL_CODE)));
      underTest.lookup(GERMAN_ADDRESS);
      verify(statistics)
          .recordLookupComparison(false, Statistics.LOOKUP_COMPARISON_RESULT_FUZZY_WINS);
    }

    @Test
    void thatExactMatchIsRecognized() {
      when(lookupTree.lookupHealthOffice(any())).thenReturn(Optional.empty());
      when(lookupMaps.getValue(any(), any())).thenReturn(HEALTH_OFFICE);
      underTest.lookup(GERMAN_ADDRESS);
      verify(statistics)
          .recordLookupComparison(false, Statistics.LOOKUP_COMPARISON_RESULT_EXACT_WINS);
    }
  }

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
