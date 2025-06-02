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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import de.gematik.demis.nrs.service.dto.LookupAddress;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LookupTreeTest {

  @Test
  void canLookupHealthOfficeZipOnly() {
    final LookupTree lookupTree =
        LookupTree.builder().add(new LookupAddress("a", "zip1", null, null, null, null)).build();

    Optional<LookupTree.LookupResult> result =
        lookupTree.lookupHealthOffice(new LookupTree.LookupRequest("zip1", null, null, null, null));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.POSTAL_CODE));

    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "nr", "ex"));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.POSTAL_CODE));
  }

  @Test
  void canLookupHealthOfficeZipAndCity() {
    final LookupTree lookupTree =
        LookupTree.builder().add(new LookupAddress("a", "zip1", "city", null, null, null)).build();

    Optional<LookupTree.LookupResult> result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", null, null, null));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.CITY));

    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "nr", "ex"));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.CITY));

    result =
        lookupTree.lookupHealthOffice(new LookupTree.LookupRequest("zip1", null, null, null, null));
    assertThat(result).isEmpty();
  }

  @Test
  void canLookupHealthOfficeZipAndCityAndStreet() {
    final LookupTree lookupTree =
        LookupTree.builder()
            // In case we have an issue with the order of adding items here, so we want to verify
            // that we
            // always find the most precise match
            .add(new LookupAddress("a", "zip1", "city", "street", null, null))
            .build();

    Optional<LookupTree.LookupResult> result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", null, null));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.STREET));

    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "nr", "ex"));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.STREET));

    result =
        lookupTree.lookupHealthOffice(new LookupTree.LookupRequest("zip1", null, null, null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", null, null, null));
    assertThat(result).isEmpty();
  }

  @Test
  void canLookupHealthOfficeDownToNumber() {
    final LookupTree lookupTree =
        LookupTree.builder()
            // In case we have an issue with the order of adding items here, so we want to verify
            // that we
            // always find the most precise match
            .add(new LookupAddress("a", "zip1", "city", "street", "1", null))
            .build();

    Optional<LookupTree.LookupResult> result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "1", null));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.NUMBER));

    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "1", "ex"));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.NUMBER));

    result =
        lookupTree.lookupHealthOffice(new LookupTree.LookupRequest("zip1", null, null, null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", null, null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "2", null));
    assertThat(result).isEmpty();
  }

  @Test
  void canLookupHealthOfficeDownToExtension() {
    final LookupTree lookupTree =
        LookupTree.builder()
            // In case we have an issue with the order of adding items here, so we want to verify
            // that we
            // always find the most precise match
            .add(new LookupAddress("a", "zip1", "city", "street", "1", "/1"))
            .build();

    Optional<LookupTree.LookupResult> result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "1", "/1"));
    assertThat(result).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.EXTENSION));

    result =
        lookupTree.lookupHealthOffice(new LookupTree.LookupRequest("zip1", null, null, null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", null, null, null));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street2", null, "/1"));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "2", "/1"));
    assertThat(result).isEmpty();
    result =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city", "street", "1", "a"));
    assertThat(result).isEmpty();
  }

  @Test
  void canFindSiblingsOnCityLevel() {
    final LookupTree lookupTree =
        LookupTree.builder()
            .add(new LookupAddress("a", "zip1", "city11", null, null, null))
            .add(new LookupAddress("b", "zip1", "city12", null, null, null))
            .build();

    final Optional<LookupTree.LookupResult> perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city2", null, null, null));
    assertThat(perfectMatch).contains(new LookupTree.LookupResult("b", 1, LookupTree.Level.CITY));
  }

  @Test
  void canFindSiblingsOnStreetLevel() {
    final LookupTree lookupTree =
        LookupTree.builder()
            // best match!
            .add(new LookupAddress("a", "zip1", "city", "street", null, null))
            // Should never match due to high edit distance in street
            .add(new LookupAddress("b", "zip1", "city2", "boulevard", null, null))
            // Should never match due to high edit distance in city
            .add(new LookupAddress("c", "zip1", "city12", "street", null, null))
            .build();

    final Optional<LookupTree.LookupResult> perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("zip1", "city2", "street", null, null));
    assertThat(perfectMatch).contains(new LookupTree.LookupResult("a", 1, LookupTree.Level.STREET));
  }

  @Test
  void addingDoesntOverwritePreviousNode() {
    /*
    Verify that the underlying implementation handles adding to existing data correctly, instead of overwriting nodes.
     */
    final LookupTree lookupTree =
        LookupTree.builder()
            .add(new LookupAddress("a", "55278", "Undenheim", "An der Molkerei", "9", null))
            .add(new LookupAddress("a", "55278", "Undenheim", "Frankenstraße", "8", null))
            .add(new LookupAddress("a", "55278", "Undenheim", "Sackgasse", "1", null))
            .add(
                new LookupAddress(
                    "c", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null))
            .add(
                new LookupAddress(
                    "b", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "c"))
            .add(
                new LookupAddress(
                    "b", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "d"))
            .build();
    Optional<LookupTree.LookupResult> perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest("55278", "Undenheim", "An der Molkerei", "9", null));
    assertThat(perfectMatch).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.NUMBER));

    perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest(
                "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "c"));
    assertThat(perfectMatch)
        .contains(new LookupTree.LookupResult("b", 0, LookupTree.Level.EXTENSION));

    // not working even though it should, probably not identified as leaf because we add children
    // later, we should
    // add another check on the children
    perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest(
                "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null));
    assertThat(perfectMatch).contains(new LookupTree.LookupResult("c", 0, LookupTree.Level.NUMBER));

    // TODO: won't work either because isLeaf is not working, maybe we should attempt to set the ga
    // on
    // every level and remove it only
    // when we encounter another ga for the same level...
    perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest(
                "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null));
  }

  @Test
  void orderOfAddingDoesntMatter() {
    LookupTree lookupTree =
        LookupTree.builder()
            .add(
                new LookupAddress(
                    "b", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "c"))
            .add(
                new LookupAddress(
                    "c", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null))
            .build();

    final Optional<LookupTree.LookupResult> perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest(
                "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null));
    assertThat(perfectMatch).contains(new LookupTree.LookupResult("c", 0, LookupTree.Level.NUMBER));

    final LookupTree.Builder add =
        LookupTree.builder()
            .add(new LookupAddress("b", "12345", "city", "street"))
            .add(new LookupAddress("a", "12345", "city"));
    final LookupTree build = add.build();
    Optional<LookupTree.LookupResult> anotherMatch =
        build.lookupHealthOffice(new LookupTree.LookupRequest("12345", "city", null, null, null));
    assertThat(anotherMatch).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.CITY));
    anotherMatch =
        build.lookupHealthOffice(
            new LookupTree.LookupRequest("12345", "city", "Unknown", null, null));
    assertThat(anotherMatch).contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.CITY));
  }

  @Test
  void theMostAbstractKeyShouldWin() {
    final LookupTree tree =
        LookupTree.builder()
            // GIVEN a very specific address with a health office
            .add(new LookupAddress("a", "a", "b", "c", "d", "e"))
            // AND a less specific address with a different address
            .add(new LookupAddress("b", "a", "b", "c", "d", null))
            // AND so on
            .add(new LookupAddress("c", "a", "b", "c", null, null))
            .add(new LookupAddress("d", "a", "b", null, null, null))
            .add(new LookupAddress("e", "a", null, null, null, null))
            .build();

    // THEN we can match all parts correctly
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", null, null, null, null)))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", null, null, null)))
        .contains(new LookupTree.LookupResult("d", 0, LookupTree.Level.CITY));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", null, null)))
        .contains(new LookupTree.LookupResult("c", 0, LookupTree.Level.STREET));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", null)))
        .contains(new LookupTree.LookupResult("b", 0, LookupTree.Level.NUMBER));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", "e")))
        .contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.EXTENSION));
  }

  @Test
  void matchWithGapsWorks() {
    final LookupTree tree =
        LookupTree.builder()
            // GIVEN a very specific address with a health office
            .add(new LookupAddress("a", "a", "b", "c", "d", "e"))
            // AND a less specific address with a different address
            .add(new LookupAddress("e", "a", null, null, null, null))
            .build();

    // THEN we always match the last known parent for gaps
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", null, null, null, null)))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", null, null, null)))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", null, null)))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", null)))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    // AND we still match with precision if we can
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", "e")))
        .contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.EXTENSION));
    // AND we can match with incorrect data
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "X", "X", "X", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "X", "X", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "X", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.POSTAL_CODE));
  }

  @Test
  void matchWithGapsOnCity() {
    final LookupTree tree =
        LookupTree.builder()
            // GIVEN a very specific address with a health office
            .add(new LookupAddress("a", "a", "b", "c", "d", "e"))
            // AND a less specific address with a different address
            .add(new LookupAddress("e", "a", "b", null, null, null))
            .build();

    // the highest info we have is at city level, everything above we can't know

    // THEN we always match the last known parent for gaps
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", null, null, null, null)))
        .isEmpty();
    assertThat(
            tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "XXXXX", null, null, null)))
        .isEmpty();
    // AND we still match with precision if we can
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", "e")))
        .contains(new LookupTree.LookupResult("a", 0, LookupTree.Level.EXTENSION));
    // AND we can match with incorrect data
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "X", "X", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.CITY));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "X", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.CITY));
    assertThat(tree.lookupHealthOffice(new LookupTree.LookupRequest("a", "b", "c", "d", "X")))
        .contains(new LookupTree.LookupResult("e", 0, LookupTree.Level.CITY));
  }

  @Test
  void onlyExtensionsAreKnownButExtensionIsMissing() {
    final LookupTree lookupTree =
        LookupTree.builder()
            .add(
                new LookupAddress(
                    "b", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "c"))
            .add(
                new LookupAddress(
                    "c", "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", "a"))
            .build();

    final Optional<LookupTree.LookupResult> perfectMatch =
        lookupTree.lookupHealthOffice(
            new LookupTree.LookupRequest(
                "14712", "Rathenow", "Neufriedrichsdorfer Straße", "25", null));
    // Given that all addresses use different health offices based on the extension, we can't match
    assertThat(perfectMatch).isEmpty();
  }

  @Test
  void cantMutateABuilderAfterBuilding() {
    final LookupTree.Builder builder = LookupTree.builder();
    builder.add(new LookupAddress("a", "B"));
    builder.build();

    final LookupAddress lookupAddress = new LookupAddress("a", "C");
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(
            () -> {
              builder.add(lookupAddress);
            });
    assertThatExceptionOfType(IllegalStateException.class).isThrownBy(builder::build);
  }
}
