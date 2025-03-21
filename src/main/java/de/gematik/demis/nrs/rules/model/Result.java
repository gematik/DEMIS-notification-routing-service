package de.gematik.demis.nrs.rules.model;

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
 * #L%
 */

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.function.Predicate;

/**
 * @param description
 * @param routesTo all routes of the notifications
 * @param type
 * @param notificationCategory
 */
public record Result(
    String description,
    List<Route> routesTo,
    String type,
    String notificationCategory,
    SequencedSet<BundleAction> bundleActions) {

  /**
   * Return true if any route matches the given predicate
   *
   * @param matcher See {@link Route#receiverIsNull()} or {@link Route#hasType(RulesResultTypeEnum)}
   */
  public boolean anyRouteMatches(final Predicate<Route> matcher) {
    return routesTo.stream().anyMatch(matcher);
  }

  public RuleBasedRouteDTO toRoutingOutput(
      final @Nullable Map<AddressOriginEnum, String> healthOffices,
      final @Nullable String responsible) {
    return new RuleBasedRouteDTO(
        type(), notificationCategory(), bundleActions(), routesTo(), healthOffices, responsible);
  }

  public RuleBasedRouteDTO toRoutingOutput(
      final List<Route> routes,
      final @Nullable Map<AddressOriginEnum, String> healthOffices,
      final @Nullable String responsible) {
    return new RuleBasedRouteDTO(
        type(), notificationCategory(), bundleActions(), routes, healthOffices, responsible);
  }
}
