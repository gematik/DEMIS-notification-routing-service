package de.gematik.demis.nrs.service.futs;

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

import com.google.common.base.Supplier;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * A configurable {@link Supplier} implementation to be used with Guava. Allows you to create an
 * instance and pass it to {@link com.google.common.base.Suppliers} for memoization.
 */
public abstract class ConceptMapSupplier<K, V> implements Supplier<Map<K, V>> {

  @Nonnull private final FutsClient futsClient;
  @Nonnull private final String conceptMapKey;

  public ConceptMapSupplier(
      @Nonnull final FutsClient futsClient, @Nonnull final String conceptMapKey) {
    this.futsClient = futsClient;
    this.conceptMapKey = conceptMapKey;
  }

  @Nonnull
  protected Map<String, String> getRaw() {
    // create an immutable, non-null copy of the underlying map returned by Feign
    // otherwise a caller might modify the map by accident
    return Map.copyOf(futsClient.getConceptMap(conceptMapKey));
  }
}
