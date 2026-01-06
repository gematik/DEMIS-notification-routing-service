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
import com.google.common.base.Suppliers;
import de.gematik.demis.nrs.service.dto.HealthOfficeId;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** Provide an entry point to cached access for concept maps. */
@Slf4j
@Service
public class ConceptMapService {

  private static final Duration EXPIRE_AFTER_30_MINUTES = Duration.ofMinutes(30);

  @Nonnull private final Supplier<Map<HealthOfficeId, HealthOfficeId>> tuberculosisSupplier;

  public ConceptMapService(@Nonnull final TuberculosisConceptMapSupplier tuberculosisSupplier) {
    this.tuberculosisSupplier =
        Suppliers.memoizeWithExpiration(tuberculosisSupplier, EXPIRE_AFTER_30_MINUTES);
  }

  /**
   * Lookup a tuberculosis health office assigned to the given original health office.
   *
   * @return {@link Optional#empty()} if no mapping exists or the client couldn't retrieve the map.
   */
  @Nonnull
  public Optional<HealthOfficeId> tuberculosisHealthOfficeFor(
      @Nonnull final HealthOfficeId originalHealthOffice) {
    return Optional.ofNullable(tuberculosisSupplier.get())
        .map(conceptMap -> conceptMap.get(originalHealthOffice));
  }
}
