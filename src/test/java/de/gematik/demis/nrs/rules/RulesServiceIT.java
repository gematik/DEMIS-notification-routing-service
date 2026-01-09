package de.gematik.demis.nrs.rules;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.rules.model.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** This tests verifies the pre-7.3 behaviour and can be removed once 7.3 is ready. */
@SpringBootTest(
    classes = NotificationRoutingApplication.class,
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "nrs.routing-rules=rules/routingConfig.json",
      "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup",
      "nrs.rules-start-id=start",
      "nrs.checkWorkaroundIsWorking=false"
    })
class RulesServiceIT {

  private static final String BASE_PATH = "src/test/resources/fhir";

  @Autowired private RulesService rulesService;

  @Autowired private FhirContext fhirContext;

  private static Stream<Arguments> bundleToExpectedResultId() {
    return Stream.of(
        arguments(Path.of(BASE_PATH, "6_1/notifiedperson.json"), "notification6_1"),
        arguments(
            Path.of(BASE_PATH, "6_1/disease-notification-bundle.json"), "notification6_1_cvd"),
        arguments(Path.of(BASE_PATH, "6_1/mybd-notifiedperson.json"), "disease_6_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "6_1/mytd-notifiedperson.json"), "disease_6_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "7_1/notifiedperson.json"), "notification7_1"),
        arguments(Path.of(BASE_PATH, "7_1/cvdp-notifiedperson.json"), "notification7_1_cvd"),
        arguments(
            Path.of(BASE_PATH, "7_1/mytp-notifiedperson.json"), "laboratory_7_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "7_3/anonymous.json"), "laboratory_notification7_3_anonymous"),
        arguments(
            Path.of(BASE_PATH, "7_3/nonnominal-notifiedperson.json"), "laboratory_notification7_3"),
        arguments(
            Path.of(BASE_PATH, "7_3/nonnominal-notbyname.json"), "laboratory_notification7_3"),
        arguments(Path.of(BASE_PATH, "7_3/disease-nonnominal.json"), "disease_notification7_3"),
        // This merely records the current behaviour and is not a valid assertion, see
        // RulesService73IT for a valid assertion.
        arguments(
            Path.of(BASE_PATH, "7_3/disease-anonymous-nonnominal.json"), "disease_notification7_3"),
        arguments(Path.of(BASE_PATH, "7_4/negative-covid19-bundle.json"), "notification7_4"));
  }

  @ParameterizedTest
  @MethodSource("bundleToExpectedResultId")
  void thatRuleMatches(final Path path, final String expectedRule) throws IOException {
    final String bundleJson = Files.readString(path);
    final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    final Optional<Result> resultCandidate = rulesService.evaluateRules(bundle);
    assertThat(resultCandidate).isPresent();
    final Result result = resultCandidate.get();
    assertThat(result.id()).isEqualTo(expectedRule);
  }

  /**
   * Regression for DEMIS-3673 to ensure, that Bundles with mismatching Patient profile are not send
   * as 7.4 i.e. expected 7.1 with NotifiedPerson, but receive NotifiedPersonNotByName instead
   */
  @ParameterizedTest
  @MethodSource("invalid71Bundles")
  void thatInvalid7_1BundlesAreProcessedAs7_1(final Path path) throws IOException {
    final String bundleJson = Files.readString(path);
    final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    // Evaluate the rules
    final Optional<Result> optionalResult = rulesService.evaluateRules(bundle);
    assertThat(optionalResult).isPresent();
    final Result result = optionalResult.get();
    assertThat(result.id()).isEqualTo("notification7_1");
  }

  private static Stream<Arguments> invalid71Bundles() {
    return Stream.of(
        Arguments.of(Path.of("src/test/resources/fhir/invalid_71.json")),
        Arguments.of(
            Path.of("src/test/resources/fhir/invalid_71_multiple_observations_mixed.json")),
        Arguments.of(
            Path.of("src/test/resources/fhir/invalid_71_multiple_observations_all_neg.json")));
  }
}
