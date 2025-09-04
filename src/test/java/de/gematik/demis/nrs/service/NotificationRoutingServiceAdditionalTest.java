package de.gematik.demis.nrs.service;

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

import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_CURRENT;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_ORDINARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.BundleActionType.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.ActionType;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.service.futs.ConceptMapService;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.nrs.util.SequencedSets;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.*;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingServiceAdditionalTest {

  @Mock FhirReader fhirReaderMock;
  @Mock AddressToHealthOfficeLookup addressToHealthOfficeLookupMock;
  @Mock RulesService rulesServiceMock;
  @Mock ConceptMapService conceptMapService;
  @Mock Statistics statisticsMock;

  NotificationRoutingService notificationRoutingService;

  @BeforeEach
  void setup() {

    ReceiverResolutionService receiverResolutionService =
        new ReceiverResolutionService(addressToHealthOfficeLookupMock, conceptMapService);
    notificationRoutingService =
        new NotificationRoutingService(
            fhirReaderMock, statisticsMock, rulesServiceMock, receiverResolutionService, false);
  }

  @Test
  void determineRuleBasedRouting_SpecificReceiver() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);
    // prevent npe
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(new RoutingInput(Map.of()));

    // call to rule service
    List<Route> routeListe =
        singletonList(
            new Route(
                RulesResultTypeEnum.SPECIFIC_RECEIVER,
                "1.",
                singletonList(ActionType.NO_ACTION),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    RuleBasedRouteDTO ruleBasedRouteDTO =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, "");

    assertThat(ruleBasedRouteDTO.responsible()).isEqualTo("1.");
    assertThat(ruleBasedRouteDTO.healthOffices()).isEmpty();
  }

  @Test
  void determineRuleBasedRouting_ResponsibleHealthOffice() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        singletonList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                null,
                singletonList(ActionType.ENCRYPT),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    Map<AddressOriginEnum, AddressDTO> map = new EnumMap<>(AddressOriginEnum.class);
    AddressDTO value = new AddressDTO("street", "1", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_PRIMARY, value);
    RoutingInput routingInput = new RoutingInput(map);
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(routingInput);

    // address look up
    Optional<String> someIdOpt = Optional.of("1.0.1");
    when(addressToHealthOfficeLookupMock.lookup(value)).thenReturn(someIdOpt);

    RuleBasedRouteDTO ruleBasedRouteDTO =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, "");

    verify(statisticsMock, times(1))
        .incHealthOfficeLookup(NOTIFIED_PERSON_PRIMARY, someIdOpt.isPresent());

    assertThat(ruleBasedRouteDTO.responsible()).isEqualTo("1.0.1");
  }

  @Test
  void determineRuleBasedRouting_ResponsibleHealthOfficeAndSormas() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        asList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                null,
                singletonList(ActionType.ENCRYPT),
                true),
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                null,
                singletonList(ActionType.ENCRYPT),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    Map<AddressOriginEnum, AddressDTO> map = new EnumMap<>(AddressOriginEnum.class);
    AddressDTO value = new AddressDTO("street", "1", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_PRIMARY, value);
    RoutingInput routingInput = new RoutingInput(map);
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(routingInput);

    // address look up
    Optional<String> someIdOpt = Optional.of("1.0.1");
    when(addressToHealthOfficeLookupMock.lookup(value)).thenReturn(someIdOpt);

    RuleBasedRouteDTO ruleBasedRouteDTO =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, "");

    verify(statisticsMock, times(2))
        .incHealthOfficeLookup(NOTIFIED_PERSON_PRIMARY, someIdOpt.isPresent());
  }

  @Test
  void determineRuleBasedRouting_NoRuledFound() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, ""))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void determineRuleBasedRouting_TestUser() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        asList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                null,
                singletonList(ActionType.ENCRYPT),
                true),
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                "1.",
                singletonList(ActionType.ENCRYPT),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    Map<AddressOriginEnum, AddressDTO> map = new EnumMap<>(AddressOriginEnum.class);
    AddressDTO value = new AddressDTO("street", "1", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_PRIMARY, value);
    RoutingInput routingInput = new RoutingInput(map);
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(routingInput);

    // address look up
    Optional<String> someIdOpt = Optional.of("1.0.1");
    when(addressToHealthOfficeLookupMock.lookup(value)).thenReturn(someIdOpt);

    RuleBasedRouteDTO ruleBasedRouteDTO =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, true, "testerId");

    assertThat(ruleBasedRouteDTO.responsible()).isEqualTo("1.0.1");
  }

  @Test
  void determineRuleBasedRouting_SpecificReceiverIdNotSet() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        singletonList(
            new Route(
                RulesResultTypeEnum.SPECIFIC_RECEIVER,
                null,
                singletonList(ActionType.NO_ACTION),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    assertThatThrownBy(
            () -> notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, ""))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void determineRuleBasedRouting_SpecificReceiverIdNull() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        asList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                null,
                singletonList(ActionType.ENCRYPT),
                true),
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                null,
                singletonList(ActionType.ENCRYPT),
                false),
            new Route(
                RulesResultTypeEnum.SPECIFIC_RECEIVER,
                null,
                singletonList(ActionType.PSEUDO_COPY),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    Map<AddressOriginEnum, AddressDTO> map = new EnumMap<>(AddressOriginEnum.class);
    AddressDTO value = new AddressDTO("street", "1", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_PRIMARY, value);
    RoutingInput routingInput = new RoutingInput(map);

    assertThatThrownBy(
            () -> notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, ""))
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void determineRuleBasedRouting_MaxRuleResult() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        asList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                null,
                singletonList(ActionType.ENCRYPT),
                true),
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                null,
                singletonList(ActionType.ENCRYPT),
                false),
            new Route(
                RulesResultTypeEnum.SPECIFIC_RECEIVER,
                "1.",
                singletonList(ActionType.PSEUDO_COPY),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    Map<AddressOriginEnum, AddressDTO> map = new EnumMap<>(AddressOriginEnum.class);
    AddressDTO value = new AddressDTO("street", "1", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_PRIMARY, value);
    AddressDTO value2 = new AddressDTO("street", "2", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_CURRENT, value2);
    AddressDTO value3 = new AddressDTO("street", "3", "12345", "Berlin", "DE");
    map.put(NOTIFIED_PERSON_ORDINARY, value3);
    RoutingInput routingInput = new RoutingInput(map);
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(routingInput);

    // address look up
    when(addressToHealthOfficeLookupMock.lookup(value2)).thenReturn(Optional.of("1.0.3"));

    RuleBasedRouteDTO ruleBasedRouteDTO =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, "");

    assertThat(ruleBasedRouteDTO.responsible()).isEqualTo("1.0.3");
    assertThat(ruleBasedRouteDTO.routes())
        .hasSize(3)
        .extracting("specificReceiverId")
        .contains("1.0.3", "1.", "2.0.3");
  }

  @Test
  void determineRuleBasedRouting_NoAdresses() {

    // fhir notification and parse notification to bundle
    String fhirNotification = "i am a fhir notification mock";
    Bundle bundle = new Bundle();
    when(fhirReaderMock.toBundle(fhirNotification)).thenReturn(bundle);

    // call to rule service
    List<Route> routeListe =
        asList(
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                null,
                singletonList(ActionType.ENCRYPT),
                true),
            new Route(
                RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                null,
                singletonList(ActionType.ENCRYPT),
                false),
            new Route(
                RulesResultTypeEnum.SPECIFIC_RECEIVER,
                "1.",
                singletonList(ActionType.PSEUDO_COPY),
                false));
    Optional<Result> optResult = getOptionalResult(routeListe);
    when(rulesServiceMock.evaluateRules(bundle)).thenReturn(optResult);

    // extract routingInput
    RoutingInput routingInput = new RoutingInput(Collections.emptyMap());
    when(fhirReaderMock.getRoutingInput(bundle)).thenReturn(routingInput);

    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(
            () -> {
              notificationRoutingService.determineRuleBasedRouting(fhirNotification, false, "");
            })
        .withMessage(ExceptionMessages.NO_HEALTH_OFFICE_FOUND);
  }

  private static Optional<Result> getOptionalResult(final List<Route> routeListe) {
    return Optional.of(
        new Result(
            "123",
            "some description",
            routeListe,
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2")));
  }
}
