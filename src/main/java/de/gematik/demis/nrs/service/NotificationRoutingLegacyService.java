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
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.SPECIFIC_RECEIVER;
import static de.gematik.demis.nrs.service.ExceptionMessages.LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_HEALTH_OFFICE_FOUND;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_OPTIONAL_HEALTH_OFFICE_FOUND;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_RESULT_FOR_RULE_EVALUATION;
import static de.gematik.demis.nrs.service.ExceptionMessages.NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dlr.DestinationLookupReaderService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationRoutingLegacyService {
  static final String LOOKUP_HEALTH_OFFICE_PREFIX_ID = "2";
  static final String LOOKUP_HEALTH_OFFICE_DELIMITER = ".";
  private static final AddressOriginEnum[] ADDRESS_LOOKUP_ORDER = {
    NOTIFIED_PERSON_CURRENT,
    NOTIFIED_PERSON_ORDINARY,
    NOTIFIED_PERSON_PRIMARY,
    NOTIFIED_PERSON_OTHER,
    SUBMITTER,
    NOTIFIER
  };

  private final FhirReader fhirReader;
  private final AddressToHealthOfficeLookup addressToHealthOfficeLookup;
  private final Statistics statistics;
  private final RulesService rulesService;
  private final DestinationLookupReaderService destinationLookupReaderService;
  private final boolean isFollowUpNotificationEnabled;

  public NotificationRoutingLegacyService(
      final FhirReader fhirReader,
      final AddressToHealthOfficeLookup addressToHealthOfficeLookup,
      final Statistics statistics,
      final RulesService rulesService,
      final DestinationLookupReaderService destinationLookupReaderService,
      @Value("${feature.flag.follow.up.notification}")
          final boolean isFollowUpNotificationEnabled) {
    this.fhirReader = fhirReader;
    this.addressToHealthOfficeLookup = addressToHealthOfficeLookup;
    this.statistics = statistics;
    this.rulesService = rulesService;
    this.destinationLookupReaderService = destinationLookupReaderService;
    this.isFollowUpNotificationEnabled = isFollowUpNotificationEnabled;
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
    RuleBasedRouteDTO returnDTO;
    final Optional<String> followUpDepartment;
    final Bundle bundle = fhirReader.toBundle(fhirBundleAsString);
    final Optional<Result> evaluatedRules = rulesService.evaluateRules(bundle);
    if (evaluatedRules.isEmpty()) {
      final String errorMessage =
          String.format(NO_RESULT_FOR_RULE_EVALUATION, bundle.getIdentifier().getValue());
      throw unprocessableEntityError(errorMessage);
    }
    Result ruleResult = evaluatedRules.get();

    if (this.isFollowUpNotificationEnabled
        && ruleResult.anyRouteMatches(Route.hasType(RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO))) {
      followUpDepartment =
          destinationLookupReaderService.getDepartmentForFollowUpNotification(
              bundle, evaluatedRules.get().type());
      boolean isDepartmentRequired =
          ruleResult.routesTo().stream()
              .filter(Route.hasType(RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO))
              .anyMatch(route -> !route.optional());
      if (isDepartmentRequired && followUpDepartment.isEmpty()) {
        throw unprocessableEntityError(ExceptionMessages.MISSING_REQUIRED_RECEIVER);
      }
    } else {
      followUpDepartment = Optional.empty();
    }

    // replace tuberculosis ones here so we dont have to change the legacy code
    final List<Route> routeStream =
        ruleResult.routesTo().stream()
            .map(
                r -> {
                  if (RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS.equals(r.type())) {
                    return new Route(
                        RESPONSIBLE_HEALTH_OFFICE,
                        r.specificReceiverId(),
                        r.actions(),
                        r.optional());
                  } else {
                    return r;
                  }
                })
            .toList();
    ruleResult =
        new Result(
            ruleResult.id(),
            ruleResult.description(),
            routeStream,
            ruleResult.type(),
            ruleResult.notificationCategory(),
            ruleResult.bundleActions(),
            ruleResult.allowedRoles());

    if (followUpDepartment.isPresent()) {
      returnDTO =
          ruleResult.toRoutingOutput(
              routeStream.stream()
                  .map(
                      r -> {
                        if (RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO.equals(r.type())) {
                          return r.copyWithReceiver(followUpDepartment.get());
                        } else {
                          return r;
                        }
                      })
                  .toList(),
              Map.of(),
              followUpDepartment.get());
    }
    // search for responsible_health_office in routing data
    else if (ruleResult.anyRouteMatches(Route.hasType(RESPONSIBLE_HEALTH_OFFICE))) {
      returnDTO = handleHealthOfficeResponsible(bundle, ruleResult);
    }
    // no responsible_health_office means that a specific receiver is responsible. right now only
    // the rki is supported as specific receiver.
    else {
      returnDTO = handleSpecificReceiver(ruleResult);
    }

    if (isTestNotification) {
      returnDTO = RuleBasedRouteDTO.rewriteForTests(returnDTO, recipientForTestRouting);
    }

    return returnDTO;
  }

  private RuleBasedRouteDTO handleSpecificReceiver(Result result) {
    if (result.anyRouteMatches(Route.receiverIsNull())) {
      final String errorMessage =
          String.format(NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE, "specific_receiver");
      throw unprocessableEntityError(errorMessage);
    }

    if (result.routesTo().isEmpty()) {
      final String errorMessage = String.format(NO_HEALTH_OFFICE_FOUND);
      throw unprocessableEntityError(errorMessage);
    }

    final Route firstRoute = result.routesTo().getFirst();
    return result.toRoutingOutput(Collections.emptyMap(), firstRoute.specificReceiverId());
  }

  private RuleBasedRouteDTO handleHealthOfficeResponsible(Bundle bundle, Result ruleResult) {
    final RoutingInput bundleRoutingInput = fhirReader.getRoutingInput(bundle);
    final List<Route> ruleRoutingList =
        ruleResult.routesTo().stream().filter(Objects::nonNull).toList();
    final List<Route> computedRoutingData =
        completeRoutingData(ruleRoutingList, bundleRoutingInput);
    final boolean containsResponsibleHealthOffice =
        computedRoutingData.stream().anyMatch(Route.hasType(RESPONSIBLE_HEALTH_OFFICE));

    if (!containsResponsibleHealthOffice) {
      return ruleResult.toRoutingOutput(computedRoutingData, null, null);
    }

    final RoutingOutput routingOutput = getRoutingResult(bundleRoutingInput);
    final Map<AddressOriginEnum, String> healthOffices = routingOutput.healthOffices();
    final String responsible = routingOutput.responsible();
    return ruleResult.toRoutingOutput(computedRoutingData, healthOffices, responsible);
  }

  private List<Route> completeRoutingData(final List<Route> routes, RoutingInput routingInput) {
    final Optional<Route> specificReceiverWithoutId =
        routes.stream()
            .filter(Route.hasType(SPECIFIC_RECEIVER))
            .filter(Route.receiverIsNull())
            .findFirst();
    if (specificReceiverWithoutId.isPresent()) {
      final String type = specificReceiverWithoutId.get().type().getCode();
      final String errorMessage =
          String.format(NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE, type);
      throw unprocessableEntityError(errorMessage);
    }

    return routes.stream()
        .map(
            route -> {
              final boolean hasReceiver = Objects.nonNull(route.specificReceiverId());
              if (hasReceiver) {
                return Optional.of(route);
              }

              try {
                Optional<String> receiverIdOpt = lookupSpecificReceiverId(route, routingInput);
                return receiverIdOpt.map(route::copyWithReceiver);
              } catch (ServiceException e) {
                log.warn(NO_HEALTH_OFFICE_FOUND, e);
                return Optional.<Route>empty();
              }
            })
        .<Route>mapMulti(Optional::ifPresent)
        .toList();
  }

  private Optional<String> lookupSpecificReceiverId(
      final Route route, final RoutingInput routingInput) {

    if (RESPONSIBLE_HEALTH_OFFICE.equals(route.type())
        || RESPONSIBLE_HEALTH_OFFICE_SORMAS.equals(route.type())) {

      final Map<AddressOriginEnum, String> healthOffices =
          lookupHealthOffices(routingInput.addresses());
      String targetHealthOffice = determineResponsibleHealthOffice(healthOffices);
      if (targetHealthOffice == null || targetHealthOffice.isBlank()) {
        if (route.optional()) {
          final String errorMessage = String.format(NO_OPTIONAL_HEALTH_OFFICE_FOUND, route.type());
          log.warn(errorMessage);
          return Optional.empty();
        }
        final String errorMessage = String.format(NO_HEALTH_OFFICE_FOUND);
        throw unprocessableEntityError(errorMessage);
      }
      String s =
          RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS.equals(route.type())
              ? (LOOKUP_HEALTH_OFFICE_PREFIX_ID
                  + targetHealthOffice.substring(
                      targetHealthOffice.indexOf(LOOKUP_HEALTH_OFFICE_DELIMITER)))
              : targetHealthOffice;
      return Optional.of(s);
    }

    final String errorMessage =
        String.format(LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED, route.type().getCode());
    throw unprocessableEntityError(errorMessage);
  }

  private Map<AddressOriginEnum, String> lookupHealthOffices(
      final Map<AddressOriginEnum, AddressDTO> addresses) {
    final Map<AddressOriginEnum, String> healthOffices = new EnumMap<>(AddressOriginEnum.class);
    for (final Map.Entry<AddressOriginEnum, AddressDTO> addressEntry : addresses.entrySet()) {
      final AddressOriginEnum addressOrigin = addressEntry.getKey();
      final Optional<String> healthOffice =
          addressToHealthOfficeLookup.lookup(addressEntry.getValue());
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

  @Nullable
  private String determineResponsibleHealthOffice(
      final Map<AddressOriginEnum, String> healthOffices) {
    final Optional<AddressOriginEnum> highestPrioAddressOrigin =
        Arrays.stream(ADDRESS_LOOKUP_ORDER).filter(healthOffices::containsKey).findFirst();
    final String healthOffice = highestPrioAddressOrigin.map(healthOffices::get).orElse(null);

    if (highestPrioAddressOrigin.isPresent()) {
      log.debug(
          "Responsible Health Office: {} (derived from address origin {})",
          healthOffice,
          highestPrioAddressOrigin.get());
      statistics.incResponsibleAddressOrigin(highestPrioAddressOrigin.get());
    } else {
      log.info("no health office is responsible");
      statistics.incNoHealthOfficeResponsible();
    }

    return healthOffice;
  }

  private RoutingOutput getRoutingResult(final RoutingInput routingInput) {
    final Map<AddressOriginEnum, String> healthOffices =
        lookupHealthOffices(routingInput.addresses());
    final String responsible = determineResponsibleHealthOffice(healthOffices);
    return new RoutingOutput(healthOffices, responsible);
  }

  private ServiceException unprocessableEntityError(final String errorMessage) {
    log.error(errorMessage);
    throw new ServiceException(UNPROCESSABLE_ENTITY, null, errorMessage);
  }
}
