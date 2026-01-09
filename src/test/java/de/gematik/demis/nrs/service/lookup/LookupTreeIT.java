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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.LookupAddress;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "nrs.lookup-data-directory=src/test/resources/lookup/maps",
      "nrs.checkWorkaroundIsWorking=false"
    })
class LookupTreeIT {

  private static final Random r = new Random(1L);

  private LookupTree tree;
  private Multimap<String, LookupTree.LookupRequest> ga;

  @Autowired private AddressToHealthOfficeLookup legacy;

  /** Note: this can be removed with the feature toggle and is only a helper for development */
  void compareWithLegacyImplementation() {
    for (Map.Entry<String, LookupTree.LookupRequest> e : ga.entries()) {
      final Optional<LookupTree.LookupResult> perfectMatch = tree.lookupHealthOffice(e.getValue());
      final Optional<String> result = legacy.lookup(toAddressDTO(e.getValue()));
      System.out.printf("Testing for %s %n", e.getValue());
      final String expected = e.getKey();
      assertThat(perfectMatch.map(LookupTree.LookupResult::healthOffice)).contains(expected);
      assertThat(result).contains(expected);
    }
  }

  private AddressDTO toAddressDTO(final LookupTree.LookupRequest value) {
    return new AddressDTO(
        value.street(),
        value.no(),
        value.postalCode(),
        value.city(),
        AddressDTO.COUNTRY_CODE_GERMANY);
  }

  @Test
  void loadRandomSample() {
    for (Map.Entry<String, LookupTree.LookupRequest> e : ga.entries()) {
      final Optional<LookupTree.LookupResult> perfectMatch = tree.lookupHealthOffice(e.getValue());
      assertThat(perfectMatch.map(LookupTree.LookupResult::healthOffice)).contains(e.getKey());
    }
  }

  @BeforeEach
  void load() throws IOException {
    final InputStream resource =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream("lookup/random-sample.csv");
    final LookupTree.Builder builder = LookupTree.builder();
    final ImmutableMultimap.Builder<String, LookupTree.LookupRequest> testInput =
        ImmutableSetMultimap.builder();

    Objects.requireNonNull(resource);
    try (final InputStreamReader is = new InputStreamReader(resource)) {
      final CSVParser parse =
          CSVFormat.Builder.create()
              .setCommentMarker('#')
              .setDelimiter(';')
              .setEscape('\\')
              .get()
              .parse(is);

      for (final CSVRecord record : parse) {
        final String postalCode = record.get(0);
        final String city = record.get(1);
        final String street = record.get(3);
        final String number = Strings.emptyToNull(record.get(4));
        final String extension = Strings.emptyToNull(record.get(5));
        final LookupAddress lookupAddress =
            new LookupAddress(record.get(6), postalCode, city, street, number, extension);

        builder.add(lookupAddress);
        testInput.put(record.get(6), createRequest(lookupAddress));
      }
    }

    ga = testInput.build();
    tree = builder.build();
  }

  private LookupTree.LookupRequest createRequest(final LookupAddress address) {
    final String[] targets = new String[] {address.getCity().get(), address.getStreet().get()};
    for (int i = 0; i < targets.length; i++) {
      final String target = targets[i];
      final int i1 = r.nextInt(0, 6);
      switch (i1) {
        case 0: // swap letters
          final char[] asChars = target.toCharArray();
          final int letterA = r.nextInt(asChars.length);
          final int letterB = r.nextInt(asChars.length);
          final char temp = asChars[letterA];
          asChars[letterA] = asChars[letterB];
          asChars[letterB] = temp;
          targets[i] = new String(asChars);
          break;
        case 1: // remove letters
          int removeAt = r.nextInt(target.length());
          targets[i] = target.substring(0, removeAt) + target.substring(removeAt);
          break;
        case 2: // add letters
          int insertAfter = r.nextInt(target.length());
          char insertChar = (char) r.nextInt(32, 127);
          targets[i] =
              target.substring(0, insertAfter) + insertChar + target.substring(insertAfter);
          break;
        default:
          break;
      }
    }

    return new LookupTree.LookupRequest(
        address.getPostalCode(),
        targets[0],
        targets[1],
        address.getNumber().orElse(null),
        address.getExtension().orElse(null));
  }
}
