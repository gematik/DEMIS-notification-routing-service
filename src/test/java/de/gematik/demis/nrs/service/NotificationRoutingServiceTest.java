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
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_OTHER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.SUBMITTER;
import static de.gematik.demis.nrs.api.dto.BundleActionType.*;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.SPECIFIC_RECEIVER;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.TEST_DEPARTMENT;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.nrs.util.SequencedSets;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Triple;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingServiceTest {

  public static final String SOME_SENDER_ID = "someSenderId";
  @Mock FhirReader fhirReader;
  @Mock AddressToHealthOfficeLookup addressToHealthOfficeLookup;
  @Mock RulesService ruleService;
  @Mock Statistics statistics;

  @InjectMocks NotificationRoutingService underTest;

  private static AddressDTO createAddress(final String postalCode) {
    return new AddressDTO(null, null, postalCode, null, AddressDTO.COUNTRY_CODE_GERMANY);
  }

  static Stream<Arguments> testArguments() {
    return Stream.of(
        Arguments.of(
            Set.of(
                NOTIFIED_PERSON_CURRENT,
                NOTIFIED_PERSON_ORDINARY,
                NOTIFIED_PERSON_PRIMARY,
                NOTIFIED_PERSON_OTHER,
                SUBMITTER,
                NOTIFIER),
            NOTIFIED_PERSON_CURRENT),
        Arguments.of(
            Set.of(
                NOTIFIED_PERSON_ORDINARY,
                NOTIFIED_PERSON_PRIMARY,
                NOTIFIED_PERSON_OTHER,
                SUBMITTER,
                NOTIFIER),
            NOTIFIED_PERSON_ORDINARY),
        Arguments.of(
            Set.of(NOTIFIED_PERSON_PRIMARY, NOTIFIED_PERSON_OTHER, SUBMITTER, NOTIFIER),
            NOTIFIED_PERSON_PRIMARY),
        Arguments.of(Set.of(NOTIFIED_PERSON_OTHER, SUBMITTER, NOTIFIER), NOTIFIED_PERSON_OTHER),
        Arguments.of(Set.of(SUBMITTER, NOTIFIER), SUBMITTER),
        Arguments.of(Set.of(NOTIFIER), NOTIFIER));
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  void test(
      final Set<AddressOriginEnum> addressesWithHealthOffice,
      final AddressOriginEnum responsibleAddress) {
    final Map<AddressOriginEnum, AddressDTO> addresses =
        Arrays.stream(AddressOriginEnum.values())
            .collect(
                Collectors.toMap(
                    Function.identity(), type -> createAddress(String.valueOf(type.ordinal()))));
    final RoutingInput routingInput = new RoutingInput(addresses);

    final Map<AddressDTO, String> addressToHealthOfficeMap =
        addressesWithHealthOffice.stream()
            .collect(Collectors.toMap(addresses::get, type -> "GA-" + type));

    final Map<AddressOriginEnum, String> expectedHealthOffices =
        addressesWithHealthOffice.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    type -> addressToHealthOfficeMap.get(addresses.get(type))));
    final RoutingOutput expectedRoutingOutput =
        new RoutingOutput(expectedHealthOffices, expectedHealthOffices.get(responsibleAddress));

    when(addressToHealthOfficeLookup.lookup(any()))
        .then(
            answer(
                (AddressDTO address) ->
                    Optional.ofNullable(addressToHealthOfficeMap.get(address))));

    final String fhirNotification = "krasses json";
    when(fhirReader.extractRoutingInput(fhirNotification)).thenReturn(routingInput);
    final RoutingOutput routingOutput = underTest.determineRouting(fhirNotification);

    assertThat(routingOutput).isEqualTo(expectedRoutingOutput);
  }

  @Test
  void noAddresses() {
    final RoutingInput routingInput = new RoutingInput(Map.of());
    final String fhirNotification = "krasses json";
    when(fhirReader.extractRoutingInput(fhirNotification)).thenReturn(routingInput);
    final RoutingOutput routingOutput = underTest.determineRouting(fhirNotification);

    Mockito.verifyNoInteractions(addressToHealthOfficeLookup);
    assertThat(routingOutput).isEqualTo(new RoutingOutput(Map.of(), null));
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  void determineRuleBasedRoutingDeliverCorrectData(
      final Set<AddressOriginEnum> addressesWithHealthOffice,
      final AddressOriginEnum responsibleAddress) {
    // GIVEN routing input with addresses
    final Map<AddressOriginEnum, AddressDTO> addresses =
        Arrays.stream(AddressOriginEnum.values())
            .collect(
                Collectors.toMap(
                    Function.identity(), type -> createAddress(String.valueOf(type.ordinal()))));
    final RoutingInput routingInput = new RoutingInput(addresses);
    // mock Services
    when(fhirReader.toBundle(Mockito.anyString())).thenReturn(new Bundle());
    when(fhirReader.getRoutingInput(any(Bundle.class))).thenReturn(routingInput);
    // check with and without routing output
    for (int idx = 0; idx < 2; idx++) {
      final boolean withRoutingOutput = (idx != 0);
      Triple<Result, Map<AddressDTO, String>, RuleBasedRouteDTO> testData =
          generateTestData(
              withRoutingOutput, addresses, addressesWithHealthOffice, responsibleAddress, false);
      when(ruleService.evaluateRules(any(Bundle.class)))
          .thenReturn(Optional.of(testData.getLeft()));
      when(addressToHealthOfficeLookup.lookup(any()))
          .then(
              answer(
                  (AddressDTO address) -> Optional.ofNullable(testData.getMiddle().get(address))));

      // WHEN calling rule based routing with fhir message
      final RuleBasedRouteDTO routingOutput = underTest.determineRuleBasedRouting("", false, "");

      // THEN output is equals expected output
      assertThat(routingOutput).isEqualTo(testData.getRight());
    }
  }

  @Test
  void thatRuleBasedRoutingReplacesRecipientsWithTestUser() {
    final RoutingInput routingInput =
        new RoutingInput(
            Map.of(NOTIFIED_PERSON_PRIMARY, new AddressDTO("Str", "1", "12071", "Berlin", "DE")));

    final Result originalResult =
        new Result(
            "any",
            "a placeholder result",
            List.of(
                new Route(RESPONSIBLE_HEALTH_OFFICE, "rewrite-1", List.of("no_action"), false),
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_SORMAS, "rewrite-2", List.of("no_action"), false),
                new Route(SPECIFIC_RECEIVER, "rewrite-3", List.of("no_action"), false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)));

    // this is just here to avoid an NPE
    when(fhirReader.getRoutingInput(any())).thenReturn(routingInput);
    // we care about rewriting these
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(originalResult));

    final RuleBasedRouteDTO ruleBasedRouteDTO = underTest.determineRuleBasedRouting("", true, "1.");
    assertThat(ruleBasedRouteDTO.routes()).hasSize(3);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("type")
        .containsExactlyInAnyOrder(
            RESPONSIBLE_HEALTH_OFFICE, RESPONSIBLE_HEALTH_OFFICE_SORMAS, SPECIFIC_RECEIVER);
    assertThat(ruleBasedRouteDTO.routes()).extracting("specificReceiverId").containsOnly("1.");
  }

  private Triple<Result, Map<AddressDTO, String>, RuleBasedRouteDTO> generateTestData(
      final boolean withRoutingOutput,
      final Map<AddressOriginEnum, AddressDTO> addresses,
      final Set<AddressOriginEnum> addressesWithHealthOffice,
      final AddressOriginEnum responsibleAddress,
      final boolean isTestUser) {
    final String healthOfficeId = "1.05.3.78.";
    final String expectedMissingHealthOfficeId = "2.05.3.78.";
    List<Route> resultList =
        new ArrayList<>(
            List.of(
                new Route(
                    RulesResultTypeEnum.SPECIFIC_RECEIVER, "1.", List.of("pseudo_copy"), false)));
    List<Route> outputList =
        new ArrayList<>(
            List.of(
                new Route(
                    RulesResultTypeEnum.SPECIFIC_RECEIVER, "1.", List.of("pseudo_copy"), false)));

    if (withRoutingOutput) {
      resultList.addAll(
          List.of(
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE, null, List.of("encrypt"), false),
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                  null,
                  List.of("encrypt"),
                  false)));
      outputList.addAll(
          List.of(
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                  healthOfficeId,
                  List.of("encrypt"),
                  false),
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                  expectedMissingHealthOfficeId,
                  List.of("encrypt"),
                  false)));
    }
    Result result =
        new Result(
            "123",
            "test",
            resultList,
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)));
    final Map<AddressDTO, String> addressToHealthOfficeMap =
        addressesWithHealthOffice.stream()
            .collect(Collectors.toMap(addresses::get, type -> healthOfficeId));
    final Map<AddressOriginEnum, String> expectedHealthOffices =
        addressesWithHealthOffice.stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    type -> addressToHealthOfficeMap.get(addresses.get(type))));
    final RoutingOutput expectedRoutingOutput =
        new RoutingOutput(expectedHealthOffices, expectedHealthOffices.get(responsibleAddress));
    if (isTestUser) {
      outputList =
          asList(
              new Route(TEST_DEPARTMENT, SOME_SENDER_ID, singletonList("encryption"), false),
              new Route(SPECIFIC_RECEIVER, "1.", singletonList("pseudo_copy"), false));
    }

    String responsible =
        isTestUser
            ? SOME_SENDER_ID
            : withRoutingOutput ? expectedRoutingOutput.responsible() : "1.";
    RuleBasedRouteDTO expectedOutput =
        new RuleBasedRouteDTO(
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            outputList,
            withRoutingOutput ? expectedRoutingOutput.healthOffices() : emptyMap(),
            responsible);
    return Triple.of(result, addressToHealthOfficeMap, expectedOutput);
  }

  @Test
  void determineRuleBasedRoutingFailOnEmptyRuleEvaluation() {
    // mock Services
    when(fhirReader.toBundle(Mockito.anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.empty());

    // WHEN calling with serialized bundle, THEN fails for no results by rule
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageStartingWith(ExceptionMessages.NO_RESULT_FOR_RULE_EVALUATION, (Object) null);
  }

  @Test
  void determineRuleBasedRoutingFailOnSpecificReceiverWithoutId() {
    // GIVEN result for rule evaluation
    Result result =
        new Result(
            "123",
            "test",
            List.of(
                new Route(
                    RulesResultTypeEnum.SPECIFIC_RECEIVER, null, List.of("pseudo_copy"), false)),
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)));
    // mock services
    when(fhirReader.toBundle(Mockito.anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.of(result));

    // WHEN calling with serialized bundle, THEN fails by misconfiguration of specific receiver id
    // for type specific receiver
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageStartingWith(
            String.format(
                ExceptionMessages.NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE,
                RulesResultTypeEnum.SPECIFIC_RECEIVER.getCode()));
  }

  @Test
  void determineRuleBasedRoutingFailOnNoReceiverIdFound() {
    // GIVEN routing input with addresses
    final Map<AddressOriginEnum, AddressDTO> addresses =
        Arrays.stream(AddressOriginEnum.values())
            .collect(
                Collectors.toMap(
                    Function.identity(), type -> createAddress(String.valueOf(type.ordinal()))));
    final RoutingInput routingInput = new RoutingInput(addresses);
    // AND result for rule validation
    final Result result =
        new Result(
            "123",
            "test",
            List.of(
                new Route(
                    RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                    null,
                    List.of("pseudo_copy"),
                    false)),
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)));
    // mock services
    when(fhirReader.toBundle(Mockito.anyString())).thenReturn(new Bundle());
    when(fhirReader.getRoutingInput(any(Bundle.class))).thenReturn(routingInput);
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.of(result));
    when(addressToHealthOfficeLookup.lookup(any())).thenReturn(Optional.empty());

    // WHEN calling with serialized bundle, THEN fails for no health office found on lookup
    RuleBasedRouteDTO ruleBasedRouteDTO = underTest.determineRuleBasedRouting("", false, "");

    RuleBasedRouteDTO expectedRuleBasedRouteDTO =
        new RuleBasedRouteDTO(
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            List.of(),
            null,
            null);
    assertThat(ruleBasedRouteDTO).isEqualTo(expectedRuleBasedRouteDTO);
  }
}
