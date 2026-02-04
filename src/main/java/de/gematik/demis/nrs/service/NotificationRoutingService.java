package de.gematik.demis.nrs.service;

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

import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_CURRENT;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_ORDINARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_OTHER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.SUBMITTER;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.OTHER;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.SPECIFIC_RECEIVER;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_HEALTH_OFFICE_FOUND;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_OPTIONAL_HEALTH_OFFICE_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RoutePriorityComparator;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dlr.DestinationLookupReaderService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationRoutingService {
  private static final AddressOriginEnum[] ADDRESS_LOOKUP_ORDER = {
    NOTIFIED_PERSON_CURRENT,
    NOTIFIED_PERSON_ORDINARY,
    NOTIFIED_PERSON_PRIMARY,
    NOTIFIED_PERSON_OTHER,
    SUBMITTER,
    NOTIFIER
  };

  private final FhirReader fhirReader;
  private final Statistics statistics;
  private final RulesService rulesService;
  private final ReceiverResolutionService receiverResolutionService;
  private final DestinationLookupReaderService destinationLookupReaderService;
  private final boolean isFollowUpNotificationEnabled;
  private final boolean isProcessing73enabled;

  public NotificationRoutingService(
      final FhirReader fhirReader,
      final Statistics statistics,
      final RulesService rulesService,
      final ReceiverResolutionService receiverResolutionService,
      final DestinationLookupReaderService destinationLookupReaderService,
      @Value("${feature.flag.follow.up.notification}") final boolean isFollowUpNotificationEnabled,
      @Value("${feature.flag.notifications.7.3}") final boolean isProcessing73enabled) {
    this.fhirReader = fhirReader;
    this.statistics = statistics;
    this.rulesService = rulesService;
    this.receiverResolutionService = receiverResolutionService;
    this.destinationLookupReaderService = destinationLookupReaderService;
    this.isFollowUpNotificationEnabled = isFollowUpNotificationEnabled;
    this.isProcessing73enabled = isProcessing73enabled;
  }

  /**
   * Use rule-based routing to determine the routing result.
   *
   * @param fhirBundleAsString The bundle to route.
   * @param isTestNotification true if this is a test notification
   * @param recipientForTestRouting the recipient in case of a test notification
   */
  public RuleBasedRouteDTO determineRuleBasedRouting(
      final String fhirBundleAsString,
      final boolean isTestNotification,
      final String recipientForTestRouting) {

    final Bundle bundle = fhirReader.toBundle(fhirBundleAsString);
    final Result ruleResult =
        rulesService
            .evaluateRules(bundle)
            .orElseThrow(
                () ->
                    unprocessableEntityError(ExceptionMessages.noResultForRuleEvaluation(bundle)));

    validateRoutingModel(ruleResult);

    final Optional<String> followUpDepartment;
    if (isFollowUpNotificationEnabled
        && ruleResult.anyRouteMatches(Route.hasType(RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO))) {
      followUpDepartment =
          destinationLookupReaderService.getDepartmentForFollowUpNotification(
              bundle, ruleResult.type());
    } else {
      followUpDepartment = Optional.empty();
    }

    final List<Route> processableRoutes =
        ruleResult.routesTo().stream().filter(Objects::nonNull).toList();

    final ImmutableMap<RulesResultTypeEnum, Map<AddressOriginEnum, String>>
        healthOfficesByReceiverType = resolveHealthOffices(bundle, processableRoutes);

    final List<Route> resolvedRoutes =
        processableRoutes.stream()
            .map(
                route ->
                    resolveRoute(
                        route,
                        healthOfficesByReceiverType.get(route.type()),
                        followUpDepartment.orElse(null)))
            .<Route>mapMulti(Optional::ifPresent)
            .sorted(RoutePriorityComparator.INSTANCE)
            .toList();

    ensureRoutesAreResolved(processableRoutes, resolvedRoutes);

    final Route responsibleRoute =
        resolvedRoutes.stream()
            .findFirst()
            .orElseThrow(() -> unprocessableEntityError(NO_HEALTH_OFFICE_FOUND));

    final Map<AddressOriginEnum, String> responsibleHealthOffices =
        healthOfficesByReceiverType.getOrDefault(responsibleRoute.type(), Map.of());

    RuleBasedRouteDTO returnDTO =
        ruleResult.toRoutingOutput(
            resolvedRoutes, responsibleHealthOffices, responsibleRoute.specificReceiverId());

    if (isTestNotification) {
      returnDTO = RuleBasedRouteDTO.rewriteForTests(returnDTO, recipientForTestRouting);
    }

    return returnDTO;
  }

  /**
   * Translate addresses for each type of receiver, we can use this result later when we construct
   * the result
   *
   * @param bundle
   * @param processableRoutes
   * @return
   */
  @Nonnull
  private ImmutableMap<RulesResultTypeEnum, Map<AddressOriginEnum, String>> resolveHealthOffices(
      @Nonnull final Bundle bundle, @Nonnull final List<Route> processableRoutes) {
    final RoutingInput bundleRoutingInput = fhirReader.getRoutingInput(bundle);
    final Map<AddressOriginEnum, AddressDTO> addresses = bundleRoutingInput.addresses();
    if (isProcessing73enabled) {
      Map<RulesResultTypeEnum, Map<AddressOriginEnum, String>> resolvedHealthOffices =
          new EnumMap<>(RulesResultTypeEnum.class);
      for (final Route route : processableRoutes) {
        final RulesResultTypeEnum routeType = route.type();
        if (!SPECIFIC_RECEIVER.equals(routeType)) {
          resolvedHealthOffices.put(routeType, lookupHealthOffices(routeType, addresses));
        }
      }
      return ImmutableMap.copyOf(resolvedHealthOffices);
    }
    return processableRoutes.stream()
        .collect(
            Maps.toImmutableEnumMap(
                Route::type, r -> lookupHealthOfficesRegression(r.type(), addresses)));
  }

  private void ensureRoutesAreResolved(
      @Nonnull final List<Route> processableRoutes, @Nonnull final List<Route> resolvedRoutes) {
    if (processableRoutes.size() == resolvedRoutes.size()) {
      return;
    }

    final List<Route> expectedRequiredRoutes =
        processableRoutes.stream().filter(Predicate.not(Route::optional)).toList();

    final List<Route> actualRequiredRoutes =
        resolvedRoutes.stream().filter(Predicate.not(Route::optional)).toList();

    if (expectedRequiredRoutes != actualRequiredRoutes) {
      throw unprocessableEntityError(NO_HEALTH_OFFICE_FOUND);
    }

    // assumes we can always resolve SPECIFIC_RECEIVER, otherwise we'd need to get creative to
    // distinguish multiple SPECIFIC_RECEIVERs here
    final Set<RulesResultTypeEnum> expectedOptionalRoutes =
        processableRoutes.stream()
            .filter(Route::optional)
            .filter(Predicate.not(Route.hasType(SPECIFIC_RECEIVER)))
            .map(Route::type)
            .collect(Collectors.toSet());

    final Set<RulesResultTypeEnum> actualOptionalRoutes =
        actualRequiredRoutes.stream()
            .filter(Route::optional)
            .filter(Predicate.not(Route.hasType(SPECIFIC_RECEIVER)))
            .map(Route::type)
            .collect(Collectors.toSet());

    final Sets.SetView<RulesResultTypeEnum> missingRoutes =
        Sets.difference(expectedOptionalRoutes, actualOptionalRoutes);
    for (RulesResultTypeEnum type : missingRoutes) {
      log.warn(String.format(NO_OPTIONAL_HEALTH_OFFICE_FOUND, type));
    }
  }

  /**
   * Attempt to resolve {@link Route#specificReceiverId} of the given route
   *
   * @param route Route to resolve
   * @param healthOffices Health offices resolved for the receiver type
   * @return The route if it could be resolved or {@link Optional#empty()} if not
   * @throws ServiceException in case required routes can't be resolved or similar issues are
   *     encountered
   */
  @Nonnull
  private Optional<Route> resolveRoute(
      @Nonnull final Route route,
      @Nullable final Map<AddressOriginEnum, String> healthOffices,
      @Nullable final String initialDepartment) {

    if (RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO.equals(route.type())) {
      if (initialDepartment != null) {
        return Optional.of(route.copyWithReceiver(initialDepartment));
      } else {
        return Optional.empty();
      }
    }
    if (SPECIFIC_RECEIVER.equals(route.type())) {
      return Optional.of(route);
    }

    if (healthOffices == null) {
      return Optional.empty();
    }
    final Optional<AddressOriginEnum> highestPrioAddressOrigin =
        Arrays.stream(ADDRESS_LOOKUP_ORDER).filter(healthOffices::containsKey).findFirst();
    return highestPrioAddressOrigin
        .map(healthOffices::get)
        .map(
            healthOffice -> {
              log.debug(
                  "Responsible Health Office: {} (derived from address origin {})",
                  healthOffice,
                  highestPrioAddressOrigin.get());
              statistics.incResponsibleAddressOrigin(highestPrioAddressOrigin.get());
              return healthOffice;
            })
        .or(
            () -> {
              log.info("no health office is responsible");
              statistics.incNoHealthOfficeResponsible();
              return Optional.empty();
            })
        .map(route::copyWithReceiver);
  }

  /** Basic sanity checks. In the future we can replace this with static code analysis. */
  private void validateRoutingModel(@Nonnull final Result routingModel) {
    if (routingModel.routesTo().isEmpty()) {
      final String errorMessage = String.format(NO_HEALTH_OFFICE_FOUND);
      throw unprocessableEntityError(errorMessage);
    }

    routingModel.routesTo().stream()
        .filter(Route.hasType(SPECIFIC_RECEIVER))
        .filter(Route.receiverIsNull())
        .findAny()
        .ifPresent(
            route -> {
              throw unprocessableEntityError(ExceptionMessages.invalidReceiver(route.type()));
            });

    routingModel.routesTo().stream()
        .filter(Route.hasType(OTHER))
        .findFirst()
        .ifPresent(
            route -> {
              throw unprocessableEntityError(ExceptionMessages.unsupportedType(route.type()));
            });
  }

  @Nonnull
  private Map<AddressOriginEnum, String> lookupHealthOfficesRegression(
      @Nonnull final RulesResultTypeEnum type,
      @Nonnull final Map<AddressOriginEnum, AddressDTO> addresses) {
    if (SPECIFIC_RECEIVER.equals(type)) {
      return Map.of();
    }
    return lookupHealthOffices(type, addresses);
  }

  @Nonnull
  private Map<AddressOriginEnum, String> lookupHealthOffices(
      @Nonnull final RulesResultTypeEnum type,
      @Nonnull final Map<AddressOriginEnum, AddressDTO> addresses) {

    final Map<AddressOriginEnum, String> healthOffices = new EnumMap<>(AddressOriginEnum.class);
    for (final Map.Entry<AddressOriginEnum, AddressDTO> addressEntry : addresses.entrySet()) {
      final AddressOriginEnum addressOrigin = addressEntry.getKey();
      final Optional<String> healthOffice =
          receiverResolutionService.compute(type, addressEntry.getValue());
      if (healthOffice.isPresent()) {
        healthOffices.put(addressOrigin, healthOffice.get());
        log.debug("health office for '{}': {}", addressOrigin, healthOffice.get());
      } else {
        log.info("health office for address origin '{}' not found", addressOrigin);
      }
      statistics.incHealthOfficeLookup(addressOrigin, healthOffice.isPresent());
    }
    return Collections.unmodifiableMap(healthOffices);
  }

  private ServiceException unprocessableEntityError(final String errorMessage) {
    throw new ServiceException(UNPROCESSABLE_ENTITY, null, errorMessage);
  }
}
