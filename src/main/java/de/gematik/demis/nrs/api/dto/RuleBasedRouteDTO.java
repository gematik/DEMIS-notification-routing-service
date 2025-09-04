package de.gematik.demis.nrs.api.dto;

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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;
import java.util.Set;

/**
 * Information about a matched notification
 *
 * @param type Type of notification that was matched, typically the paragraph from the law
 * @param notificationCategory Broader type of notification that was matched, e.g. laboratory or
 *     disease
 * @param bundleActions A set of bundle actions that should be processed
 * @param routes A list of routes to process
 * @param healthOffices A map of health offices by the address they were resolved for
 * @param responsible The single responsible recipient for the matched notification
 * @param allowedRoles A set of roles that are allowed to send the matched notification
 */
public record RuleBasedRouteDTO(
    String type,
    String notificationCategory,
    SequencedSet<BundleAction> bundleActions,
    List<Route> routes,
    @Nullable Map<AddressOriginEnum, String> healthOffices,
    @Nullable String responsible,
    @Nonnull Set<String> allowedRoles,
    @Nullable String custodian) {

  public static String CUSTODIAN_KEY = "custodian";
  public static String ALLOWED_ROLES_KEY = "allowedRoles";

  /**
   * @return an instance with specific fields rewritten for test notifications
   */
  @Nonnull
  public static RuleBasedRouteDTO rewriteForTests(
      @Nonnull final RuleBasedRouteDTO returnDTO, @Nonnull final String recipientForTestRouting) {
    final List<Route> rewrittenRoutes =
        returnDTO.routes().stream().map(r -> r.copyWithReceiver(recipientForTestRouting)).toList();
    return new RuleBasedRouteDTO(
        returnDTO.type(),
        returnDTO.notificationCategory(),
        returnDTO.bundleActions(),
        rewrittenRoutes,
        returnDTO.healthOffices(),
        returnDTO.responsible(),
        returnDTO.allowedRoles(),
        recipientForTestRouting);
  }

  /**
   * @return An unmodifiable view of the rule based route with all keys that are NOT affected by
   *     feature flags
   */
  @Nonnull
  public Map<String, Object> toMap() {
    // Map.of() etc. forbid null values
    final Map<String, Object> result = new HashMap<>();
    result.put("type", type);
    result.put("notificationCategory", notificationCategory);
    result.put("bundleActions", bundleActions);
    result.put("routes", routes);
    result.put("healthOffices", healthOffices);
    result.put("responsible", responsible);
    return Collections.unmodifiableMap(result);
  }
}
