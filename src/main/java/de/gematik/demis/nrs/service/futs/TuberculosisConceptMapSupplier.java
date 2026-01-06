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

import com.google.common.collect.ImmutableMap;
import de.gematik.demis.nrs.service.dto.HealthOfficeId;
import java.util.Map;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * A {@link ConceptMapSupplier} implementation to fetch the tuberculosis concept map. In case the
 * FUTS returns an error the previous result is returned once. This gives the system a second
 * caching period to resolve issues, when using a memoization strategy.
 *
 * <p>Note: this class is not thread-safe and users have to ensure thread-safety themselves
 */
@Slf4j
@Service
public class TuberculosisConceptMapSupplier
    extends ConceptMapSupplier<HealthOfficeId, HealthOfficeId> {

  private static final String CONCEPT_MAP_KEY = "ReportingSiteToTuberculosisCenter";

  @CheckForNull private Map<HealthOfficeId, HealthOfficeId> previous = null;

  public TuberculosisConceptMapSupplier(@Nonnull final FutsClient futsClient) {
    super(futsClient, CONCEPT_MAP_KEY);
  }

  @Override
  public Map<HealthOfficeId, HealthOfficeId> get() {
    final Map<String, String> raw;
    try {
      raw = getRaw();
    } catch (Exception e) {
      log.error("Could not retrieve tuberculosis mapping from FUTS", e);
      final Map<HealthOfficeId, HealthOfficeId> result =
          Objects.requireNonNullElse(previous, Map.of());
      previous = Map.of();
      return result;
    }

    final ImmutableMap.Builder<HealthOfficeId, HealthOfficeId> intermediate =
        ImmutableMap.builder();
    for (Map.Entry<String, String> entry : raw.entrySet()) {
      final HealthOfficeId key = safeParse(entry.getKey());
      final HealthOfficeId value = safeParse(entry.getValue());
      if (key != null && value != null) {
        intermediate.put(key, value);
      }
    }

    final ImmutableMap<HealthOfficeId, HealthOfficeId> result = intermediate.build();
    previous = result; // 'cache' this for the next call
    return result;
  }

  @CheckForNull
  private HealthOfficeId safeParse(@Nonnull final String value) {
    try {
      return HealthOfficeId.from(value);
    } catch (IllegalArgumentException e) {
      log.warn("String '{}' can't be mapped to health office id", value, e);
      return null;
    }
  }
}
