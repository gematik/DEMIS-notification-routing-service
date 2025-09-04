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
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.*;
import static de.gematik.demis.nrs.service.ExceptionMessages.LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_HEALTH_OFFICE_FOUND;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_RESULT_FOR_RULE_EVALUATION;
import static de.gematik.demis.nrs.service.ExceptionMessages.NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.ActionType;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.nrs.util.SequencedSets;
import de.gematik.demis.service.base.error.ServiceException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingServiceLegacyTest {

  public static final String SOME_SENDER_ID = "someSenderId";
  @Mock FhirReader fhirReader;
  @Mock AddressToHealthOfficeLookup addressToHealthOfficeLookup;
  @Mock RulesService ruleService;
  @Mock Statistics statistics;

  @InjectMocks NotificationRoutingLegacyService underTest;

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
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(fhirReader.getRoutingInput(any(Bundle.class))).thenReturn(routingInput);
    // check with and without routing output
    for (int idx = 0; idx < 2; idx++) {
      final boolean withRoutingOutput = (idx != 0);
      Triple<Result, Map<AddressDTO, String>, RuleBasedRouteDTO> testData =
          generateTestData(
              withRoutingOutput, addresses, addressesWithHealthOffice, responsibleAddress);
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
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE, "rewrite-1", List.of(ActionType.NO_ACTION), false),
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                    "rewrite-2",
                    List.of(ActionType.NO_ACTION),
                    false),
                new Route(SPECIFIC_RECEIVER, "rewrite-3", List.of(ActionType.NO_ACTION), false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)),
            Set.of("role_1", "role_2"));

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

  @Test
  void thatTuberculosisRecipientsDontBreakTheService() {
    final RoutingInput routingInput =
        new RoutingInput(
            Map.of(NOTIFIED_PERSON_PRIMARY, new AddressDTO("Str", "1", "12071", "Berlin", "DE")));

    final Result originalResult =
        new Result(
            "any",
            "a placeholder result",
            List.of(
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS,
                    null,
                    List.of(ActionType.NO_ACTION),
                    false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)),
            Set.of("role_1", "role_2"));

    // this is just here to avoid an NPE
    when(fhirReader.getRoutingInput(any())).thenReturn(routingInput);
    // we care about rewriting these
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(originalResult));
    when(addressToHealthOfficeLookup.lookup(any())).thenReturn(Optional.of("1.2.3.4."));

    final RuleBasedRouteDTO ruleBasedRouteDTO =
        underTest.determineRuleBasedRouting("", false, null);
    assertThat(ruleBasedRouteDTO.routes()).hasSize(1);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("type")
        // We rewrite tuberculosis to health office to keep it simple
        .containsExactlyInAnyOrder(RESPONSIBLE_HEALTH_OFFICE);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("specificReceiverId")
        .containsOnly("1.2.3.4.");
  }

  private Triple<Result, Map<AddressDTO, String>, RuleBasedRouteDTO> generateTestData(
      final boolean withRoutingOutput,
      final Map<AddressOriginEnum, AddressDTO> addresses,
      final Set<AddressOriginEnum> addressesWithHealthOffice,
      final AddressOriginEnum responsibleAddress) {
    final String healthOfficeId = "1.05.3.78.";
    final String expectedMissingHealthOfficeId = "2.05.3.78.";
    List<Route> resultList =
        new ArrayList<>(
            List.of(
                new Route(
                    RulesResultTypeEnum.SPECIFIC_RECEIVER,
                    "1.",
                    List.of(ActionType.PSEUDO_COPY),
                    false)));
    List<Route> outputList =
        new ArrayList<>(
            List.of(
                new Route(
                    RulesResultTypeEnum.SPECIFIC_RECEIVER,
                    "1.",
                    List.of(ActionType.PSEUDO_COPY),
                    false)));

    if (withRoutingOutput) {
      resultList.addAll(
          List.of(
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                  null,
                  List.of(ActionType.ENCRYPT),
                  false),
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                  null,
                  List.of(ActionType.ENCRYPT),
                  false)));
      outputList.addAll(
          List.of(
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
                  healthOfficeId,
                  List.of(ActionType.ENCRYPT),
                  false),
              new Route(
                  RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
                  expectedMissingHealthOfficeId,
                  List.of(ActionType.ENCRYPT),
                  false)));
    }
    Result result =
        new Result(
            "123",
            "test",
            resultList,
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2"));
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

    String responsible = withRoutingOutput ? expectedRoutingOutput.responsible() : "1.";
    RuleBasedRouteDTO expectedOutput =
        new RuleBasedRouteDTO(
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            outputList,
            withRoutingOutput ? expectedRoutingOutput.healthOffices() : emptyMap(),
            responsible,
            Set.of("role_1", "role_2"),
            null);
    return Triple.of(result, addressToHealthOfficeMap, expectedOutput);
  }

  @Test
  void determineRuleBasedRoutingFailOnEmptyRuleEvaluation() {
    // mock Services
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.empty());

    // WHEN calling with serialized bundle, THEN fails for no results by rule
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageStartingWith(NO_RESULT_FOR_RULE_EVALUATION, (Object) null);
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
                    RulesResultTypeEnum.SPECIFIC_RECEIVER,
                    null,
                    List.of(ActionType.PSEUDO_COPY),
                    false)),
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2"));
    // mock services
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.of(result));

    // WHEN calling with serialized bundle, THEN fails by misconfiguration of specific receiver id
    // for type specific receiver
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageStartingWith(
            String.format(
                NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE,
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
                    List.of(ActionType.PSEUDO_COPY),
                    false)),
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2"));
    // mock services
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
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
            null,
            Set.of("role_1", "role_2"),
            null);
    assertThat(ruleBasedRouteDTO).isEqualTo(expectedRuleBasedRouteDTO);
  }

  @Test
  void determineRuleBasedRoutingThrowsExceptionWhenNoResultFromRules() {
    // Mock no result from rules service
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any(Bundle.class))).thenReturn(Optional.empty());

    // Assert the exception
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining(String.format(NO_RESULT_FOR_RULE_EVALUATION, "null"));
  }

  @Test
  void lookupSpecificReceiverIdThrowsForUnsupportedRuleResultType() throws Exception {
    // Get private method
    Method lookupMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "lookupSpecificReceiverId", Route.class, RoutingInput.class);
    lookupMethod.setAccessible(true);

    // Create Route with unsupported type
    Route route = new Route(OTHER, null, List.of(), false);
    RoutingInput input = new RoutingInput(Map.of());

    // Verify exception is thrown
    assertThatThrownBy(() -> lookupMethod.invoke(underTest, route, input))
        .hasCauseInstanceOf(ServiceException.class)
        .cause()
        .hasMessageContaining(
            String.format(LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED, OTHER.getCode()));
  }

  @Test
  void lookupSpecificReceiverIdThrowsForNonOptionalRouteWithMissingHealthOffice() throws Exception {
    // Get private method
    Method lookupMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "lookupSpecificReceiverId", Route.class, RoutingInput.class);
    lookupMethod.setAccessible(true);

    // Create non-optional Route
    Route route = new Route(RESPONSIBLE_HEALTH_OFFICE, null, List.of(), false);
    RoutingInput input = new RoutingInput(Map.of());

    // Verify exception is thrown
    assertThatThrownBy(() -> lookupMethod.invoke(underTest, route, input))
        .hasCauseInstanceOf(ServiceException.class)
        .cause()
        .hasMessageContaining(NO_HEALTH_OFFICE_FOUND);
  }

  @Test
  void completeRoutingDataThrowsForSpecificReceiverWithoutId() throws Exception {
    // Get private method
    Method completeMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "completeRoutingData", List.class, RoutingInput.class);
    completeMethod.setAccessible(true);

    // Create Route with null ID
    List<Route> routes = List.of(new Route(SPECIFIC_RECEIVER, null, List.of(), false));
    RoutingInput input = new RoutingInput(Map.of());

    // Verify exception is thrown
    assertThatThrownBy(() -> completeMethod.invoke(underTest, routes, input))
        .hasCauseInstanceOf(ServiceException.class)
        .cause()
        .hasMessageContaining(
            String.format(
                NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE, "specific_receiver"));
  }

  @Test
  void handleSpecificReceiverThrowsExceptionWhenRoutesToIsEmpty() throws Exception {
    // Get private method
    Method handleSpecificReceiverMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "handleSpecificReceiver", Result.class);
    handleSpecificReceiverMethod.setAccessible(true);

    // Create Result with empty routes list
    Result result =
        new Result(
            "123",
            "test",
            List.of() /* empty routes */,
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2"));

    // Verify exception is thrown with correct message
    assertThatThrownBy(() -> handleSpecificReceiverMethod.invoke(underTest, result))
        .hasCauseInstanceOf(ServiceException.class)
        .extracting(throwable -> ((InvocationTargetException) throwable).getTargetException())
        .isInstanceOf(ServiceException.class);
  }

  @Test
  void handleHealthOfficeResponsibleReturnsWithNullsWhenNoResponsibleHealthOffice()
      throws Exception {
    // Get private method
    Method handleHealthOfficeResponsibleMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "handleHealthOfficeResponsible", Bundle.class, Result.class);
    handleHealthOfficeResponsibleMethod.setAccessible(true);

    // Create Bundle and Result with routes that don't have RESPONSIBLE_HEALTH_OFFICE
    Bundle bundle = new Bundle();
    Result result =
        new Result(
            "123",
            "test",
            List.of(
                new Route(SPECIFIC_RECEIVER, "receiver-id", List.of(ActionType.ENCRYPT), false)),
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            Set.of("role_1", "role_2"));

    // Mock required behavior
    when(fhirReader.getRoutingInput(any(Bundle.class))).thenReturn(new RoutingInput(Map.of()));

    // Invoke private method
    RuleBasedRouteDTO output =
        (RuleBasedRouteDTO) handleHealthOfficeResponsibleMethod.invoke(underTest, bundle, result);

    // Verify null values in output
    assertThat(output.healthOffices()).isNull();
    assertThat(output.responsible()).isNull();
  }

  @Test
  void lookupSpecificReceiverIdThrowsForUnsupportedType() throws Exception {
    // Get private method
    Method lookupMethod =
        NotificationRoutingLegacyService.class.getDeclaredMethod(
            "lookupSpecificReceiverId", Route.class, RoutingInput.class);
    lookupMethod.setAccessible(true);

    // Create Route with unsupported type (neither RESPONSIBLE_HEALTH_OFFICE nor
    // RESPONSIBLE_HEALTH_OFFICE_SORMAS)
    Route route = new Route(RulesResultTypeEnum.OTHER, "test-id", List.of(), false);
    RoutingInput input = new RoutingInput(Map.of());

    // Verify exception is thrown with correct message about unsupported type
    assertThatThrownBy(() -> lookupMethod.invoke(underTest, route, input))
        .hasCauseInstanceOf(ServiceException.class)
        .extracting(throwable -> ((InvocationTargetException) throwable).getTargetException())
        .isInstanceOf(ServiceException.class);
  }
}
