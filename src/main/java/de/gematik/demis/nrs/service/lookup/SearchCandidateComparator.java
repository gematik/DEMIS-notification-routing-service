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

import java.util.Comparator;
import javax.annotation.Nonnull;

/**
 * A comparator that sorts {@link de.gematik.demis.nrs.service.lookup.LookupTree.SearchCandidate}
 * the higher level ordinal first and then by smallest edit distance. Null elements are not
 * supported.
 */
public class SearchCandidateComparator implements Comparator<LookupTree.SearchCandidate> {

  /** Sort the smallest edit distance first */
  private static final Comparator<LookupTree.SearchCandidate> EDIT_DISTANCE_ASC =
      Comparator.comparingInt(LookupTree.SearchCandidate::editDistance);

  /** Sort more precise nodes first (e.g. street before city) */
  private static final Comparator<LookupTree.SearchCandidate> MOST_PRECISE_FIRST =
      Comparator.<LookupTree.SearchCandidate>comparingInt(c -> c.node().level().ordinal())
          .reversed();

  private static final Comparator<LookupTree.SearchCandidate> SEARCH_CANDIDATE_COMPARATOR =
      MOST_PRECISE_FIRST.thenComparing(EDIT_DISTANCE_ASC);

  @Override
  public int compare(
      @Nonnull final LookupTree.SearchCandidate o1, @Nonnull final LookupTree.SearchCandidate o2) {
    return SEARCH_CANDIDATE_COMPARATOR.compare(o1, o2);
  }
}
