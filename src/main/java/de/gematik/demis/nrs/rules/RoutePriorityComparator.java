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

import de.gematik.demis.nrs.rules.model.Route;
import java.util.Comparator;
import javax.annotation.Nonnull;

public class RoutePriorityComparator implements Comparator<Route> {

  public static final RoutePriorityComparator INSTANCE = new RoutePriorityComparator();

  private static final Comparator<Route> PRIORITY_COMPARATOR =
      Comparator.comparing(RoutePriorityComparator::routePriority);

  private static int routePriority(@Nonnull Route route) {
    return route.type().getPriority();
  }

  @Override
  public int compare(@Nonnull final Route o1, @Nonnull final Route o2) {
    return PRIORITY_COMPARATOR.compare(o1, o2);
  }
}
