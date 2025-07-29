package de.gematik.demis.nrs.rules;

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

import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.*;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class RoutePriorityComparatorTest {

  @Test
  void thatItSortsCorrectly() {
    final List<Route> other =
        List.of(
            route(OTHER),
            route(RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO),
            route(RESPONSIBLE_HEALTH_OFFICE),
            route(SPECIFIC_RECEIVER),
            route(RESPONSIBLE_HEALTH_OFFICE_SORMAS),
            route(RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS));
    final List<Route> sorted = other.stream().sorted(RoutePriorityComparator.INSTANCE).toList();
    assertThat(sorted)
        .extracting(Route::type)
        .containsExactly(
            RESPONSIBLE_HEALTH_OFFICE,
            RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS,
            RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO,
            RESPONSIBLE_HEALTH_OFFICE_SORMAS,
            SPECIFIC_RECEIVER,
            OTHER);
  }

  @Nonnull
  private static Route route(@Nonnull final RulesResultTypeEnum type) {
    return new Route(type, "", List.of(), false);
  }
}
