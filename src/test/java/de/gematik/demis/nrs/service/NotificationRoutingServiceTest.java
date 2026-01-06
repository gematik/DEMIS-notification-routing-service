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
import static de.gematik.demis.nrs.api.dto.BundleActionType.CREATE_PSEUDONYM_RECORD;
import static de.gematik.demis.nrs.api.dto.BundleActionType.NO_ACTION;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.OTHER;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.SPECIFIC_RECEIVER;
import static de.gematik.demis.nrs.service.ExceptionMessages.LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_HEALTH_OFFICE_FOUND;
import static de.gematik.demis.nrs.service.ExceptionMessages.NO_RESULT_FOR_RULE_EVALUATION;
import static de.gematik.demis.nrs.service.ExceptionMessages.NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.RulesService;
import de.gematik.demis.nrs.rules.model.ActionType;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.service.dlr.DestinationLookupReaderService;
import de.gematik.demis.nrs.service.dto.*;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.service.futs.ConceptMapService;
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
import org.hl7.fhir.r4.model.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRoutingServiceTest {

  private static final HealthOfficeId TUBERCULOSIS_SRC = HealthOfficeId.from("1.");
  private static final HealthOfficeId TUBERCULOSIS_TARGET = HealthOfficeId.from("99.");
  @Mock FhirReader fhirReader;
  @Mock AddressToHealthOfficeLookup addressToHealthOfficeLookup;
  @Mock RulesService ruleService;
  @Mock Statistics statistics;
  @Mock ConceptMapService conceptMapService;
  @Mock DestinationLookupReaderService destinationLookupReaderService;

  NotificationRoutingService underTest;
  NotificationRoutingService underTestFollowUp;

  @BeforeEach
  void setup() {
    final ReceiverResolutionService receiverResolutionService =
        new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);
    underTest =
        new NotificationRoutingService(
            fhirReader,
            statistics,
            ruleService,
            receiverResolutionService,
            destinationLookupReaderService,
            false);

    underTestFollowUp =
        new NotificationRoutingService(
            fhirReader,
            statistics,
            ruleService,
            receiverResolutionService,
            destinationLookupReaderService,
            true);
  }

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
      final RuleBasedRouteDTO actual = underTest.determineRuleBasedRouting("", false, "");

      // THEN output is equals expected output
      final RuleBasedRouteDTO expected = testData.getRight();
      assertThat(actual.bundleActions())
          .containsExactlyInAnyOrderElementsOf(expected.bundleActions());
      assertThat(actual.healthOffices()).containsAllEntriesOf(expected.healthOffices());
      assertThat(actual.notificationCategory()).isEqualTo(expected.notificationCategory());
      assertThat(actual.responsible()).isEqualTo(expected.responsible());
      assertThat(actual.routes()).containsExactlyInAnyOrderElementsOf(expected.routes());
      assertThat(actual.type()).isEqualTo(expected.type());
    }
  }

  @ParameterizedTest
  @MethodSource("testArguments")
  void determineRuleBasedRoutingDeliverCorrectDataFollowUpRegression(
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
      final RuleBasedRouteDTO actual = underTestFollowUp.determineRuleBasedRouting("", false, "");

      // THEN output is equals expected output
      final RuleBasedRouteDTO expected = testData.getRight();
      assertThat(actual.bundleActions())
          .containsExactlyInAnyOrderElementsOf(expected.bundleActions());
      assertThat(actual.healthOffices()).containsAllEntriesOf(expected.healthOffices());
      assertThat(actual.notificationCategory()).isEqualTo(expected.notificationCategory());
      assertThat(actual.responsible()).isEqualTo(expected.responsible());
      assertThat(actual.routes()).containsExactlyInAnyOrderElementsOf(expected.routes());
      assertThat(actual.type()).isEqualTo(expected.type());
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
    // use 1.<replace>, because SORMAS uses String replace methods looking for '.'
    when(addressToHealthOfficeLookup.lookup(any())).thenReturn(Optional.of("1.<replace>"));

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
      final AddressOriginEnum responsibleAddress) {
    final String healthOfficeId = "1.05.3.78.";
    final String expectedMissingHealthOfficeId = "2.05.3.78.";
    List<Route> resultList =
        new ArrayList<>(
            List.of(new Route(SPECIFIC_RECEIVER, "1.", List.of(ActionType.PSEUDO_COPY), false)));
    List<Route> outputList =
        new ArrayList<>(
            List.of(new Route(SPECIFIC_RECEIVER, "1.", List.of(ActionType.PSEUDO_COPY), false)));

    if (withRoutingOutput) {
      resultList.addAll(
          List.of(
              new Route(RESPONSIBLE_HEALTH_OFFICE, null, List.of(ActionType.ENCRYPT), false),
              new Route(
                  RESPONSIBLE_HEALTH_OFFICE_SORMAS, null, List.of(ActionType.ENCRYPT), false)));
      outputList.addAll(
          List.of(
              new Route(
                  RESPONSIBLE_HEALTH_OFFICE, healthOfficeId, List.of(ActionType.ENCRYPT), false),
              new Route(
                  RESPONSIBLE_HEALTH_OFFICE_SORMAS,
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
  void thatTuberculosisIsResolved() {
    when(conceptMapService.tuberculosisHealthOfficeFor(TUBERCULOSIS_SRC))
        .thenReturn(Optional.of(TUBERCULOSIS_TARGET));

    final AddressDTO address = new AddressDTO("Str", "1", "12071", "Berlin", "DE");
    final RoutingInput routingInput = new RoutingInput(Map.of(NOTIFIED_PERSON_PRIMARY, address));

    final Result originalResult =
        new Result(
            "",
            "",
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
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(originalResult));
    when(addressToHealthOfficeLookup.lookup(address))
        .thenReturn(Optional.of(TUBERCULOSIS_SRC.getCanonicalRepresentation()));

    final RuleBasedRouteDTO ruleBasedRouteDTO =
        underTest.determineRuleBasedRouting("", false, null);
    assertThat(ruleBasedRouteDTO.routes()).hasSize(1);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("type")
        .containsExactlyInAnyOrder(RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("specificReceiverId")
        .containsOnly(TUBERCULOSIS_TARGET.getCanonicalRepresentation());
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
            List.of(new Route(SPECIFIC_RECEIVER, null, List.of(ActionType.PSEUDO_COPY), false)),
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
                SPECIFIC_RECEIVER.getCode()));
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
                new Route(RESPONSIBLE_HEALTH_OFFICE, null, List.of(ActionType.PSEUDO_COPY), false)),
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
    assertThatThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining(NO_HEALTH_OFFICE_FOUND);
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
  void thatExceptionIsThrownWhenEncounteringInvalidReceiverType() {
    // GIVEN a required health office
    final Result value =
        new Result(
            "id",
            "desc",
            List.of(new Route(OTHER, null, List.of(), false)),
            "",
            "",
            SequencedSets.of(),
            Set.of("role_1", "role_2"));
    final Bundle bundle = new Bundle();
    bundle.setIdentifier(new Identifier().setValue("1"));
    // AND the addresses from the bundle can't be resolved to a health office
    when(fhirReader.toBundle(any())).thenReturn(bundle);
    when(ruleService.evaluateRules(bundle)).thenReturn(Optional.of(value));
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .withMessage(String.format(LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED, OTHER.getCode()));
  }

  @Test
  void thatExceptionIsThrownWhenRequiredHealthOfficeCantBeResolved() {
    // GIVEN a required health office
    final Result value =
        new Result(
            "id",
            "desc",
            List.of(new Route(RESPONSIBLE_HEALTH_OFFICE, null, List.of(), false)),
            "",
            "",
            SequencedSets.of(),
            Set.of("role_1", "role_2"));
    final Bundle bundle = new Bundle();
    bundle.setIdentifier(new Identifier().setValue("1"));
    // AND the addresses from the bundle can't be resolved to a health office
    when(fhirReader.toBundle(any())).thenReturn(bundle);
    when(fhirReader.getRoutingInput(bundle))
        .thenReturn(
            new RoutingInput(Map.of(NOTIFIED_PERSON_CURRENT, new AddressDTO("", "", "", "", ""))));
    when(addressToHealthOfficeLookup.lookup(any())).thenReturn(Optional.empty());
    when(ruleService.evaluateRules(bundle)).thenReturn(Optional.of(value));

    // THEN
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .withMessage(NO_HEALTH_OFFICE_FOUND);
  }

  @Test
  void thatModelIsInvalidForMissingSpecificReceiverId() {
    // GIVEN a routing model with a specific receiver, but without an id
    final Result value =
        new Result(
            "id",
            "desc",
            List.of(new Route(SPECIFIC_RECEIVER, null, List.of(), false)),
            "",
            "",
            SequencedSets.of(),
            Set.of("role_1", "role_2"));
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(value));

    // THEN
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .withMessage(
            String.format(
                NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE, "specific_receiver"));
  }

  @Test
  void thatModelIsInvalidForEmptyRoutes() {
    // GIVEN a routing model with a specific receiver, but without an id
    final Result value = new Result("id", "desc", List.of(), "", "", SequencedSets.of(), Set.of());
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(value));
    // THEN
    assertThatExceptionOfType(ServiceException.class)
        .isThrownBy(() -> underTest.determineRuleBasedRouting("", false, ""))
        .withMessage(NO_HEALTH_OFFICE_FOUND);
  }

  @Test
  void thatCustodianIsSetForTestUser() {
    final AddressDTO address = new AddressDTO("Str", "1", "12071", "Berlin", "DE");
    final RoutingInput routingInput = new RoutingInput(Map.of(NOTIFIED_PERSON_PRIMARY, address));

    final Result originalResult =
        new Result(
            "",
            "",
            List.of(new Route(SPECIFIC_RECEIVER, "1.", List.of(ActionType.NO_ACTION), false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)),
            Set.of("role_1", "role_2"));

    // this is just here to avoid an NPE
    when(fhirReader.getRoutingInput(any())).thenReturn(routingInput);
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(originalResult));

    final RuleBasedRouteDTO ruleBasedRouteDTO =
        underTest.determineRuleBasedRouting("", true, "testUser");
    assertThat(ruleBasedRouteDTO.custodian()).isEqualTo("testUser");
  }

  @Test
  void thatFollowUpNotificationIsSetCorrectly() {
    final RoutingInput routingInput =
        new RoutingInput(Map.of(NOTIFIER, new AddressDTO("Str", "1", "12071", "Berlin", "DE")));

    final Result followUpResult =
        new Result(
            "any",
            "a placeholder result",
            List.of(
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO,
                    null,
                    List.of(ActionType.NO_ACTION),
                    false),
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE, "rewrite-1", List.of(ActionType.NO_ACTION), false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)),
            Set.of("role_1", "role_2"));

    when(fhirReader.getRoutingInput(any())).thenReturn(routingInput);
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(followUpResult));
    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(addressToHealthOfficeLookup.lookup(any())).thenReturn(Optional.of("1.<replace>"));
    final Optional<String> department = Optional.of("testDepartment");
    when(destinationLookupReaderService.getDepartmentForFollowUpNotification(any(), any()))
        .thenReturn(department);

    final RuleBasedRouteDTO ruleBasedRouteDTO =
        underTestFollowUp.determineRuleBasedRouting("", false, "1.");

    assertThat(ruleBasedRouteDTO.routes()).hasSize(2);
    assertThat(ruleBasedRouteDTO.routes())
        .extracting("type")
        .containsExactly(RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO, RESPONSIBLE_HEALTH_OFFICE);
  }

  @Test
  void thatFollowUpNotificationWith422() {
    final Result followUpResult =
        new Result(
            "",
            "",
            List.of(
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO,
                    null,
                    List.of(ActionType.NO_ACTION),
                    false)),
            "any",
            "any",
            SequencedSets.of(BundleAction.requiredOf(NO_ACTION)),
            Set.of("role_1", "role_2"));

    when(fhirReader.toBundle(anyString())).thenReturn(new Bundle());
    when(ruleService.evaluateRules(any())).thenReturn(Optional.of(followUpResult));

    when(destinationLookupReaderService.getDepartmentForFollowUpNotification(any(), any()))
        .thenThrow(new ServiceException(UNPROCESSABLE_ENTITY, null, "Error"));

    assertThatThrownBy(() -> underTestFollowUp.determineRuleBasedRouting("", false, null))
        .isInstanceOf(ServiceException.class)
        .hasMessageStartingWith("Error");
  }
}
