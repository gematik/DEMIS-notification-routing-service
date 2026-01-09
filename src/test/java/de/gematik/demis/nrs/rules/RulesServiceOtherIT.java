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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Verify other routing configurations behind feature flags */
class RulesServiceOtherIT {

  private static final String BASE_PATH = "src/test/resources/fhir";

  private static Stream<Arguments> bundleToExpectedResultId() {
    return Stream.of(
        arguments(Path.of(BASE_PATH, "6_1/notifiedperson.json"), "disease_6_1"),
        arguments(Path.of(BASE_PATH, "6_1/disease-notification-bundle.json"), "disease_6_1_covid"),
        arguments(Path.of(BASE_PATH, "6_1/mybd-notifiedperson.json"), "disease_6_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "6_1/mytd-notifiedperson.json"), "disease_6_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "7_1/notifiedperson.json"), "laboratory_7_1"),
        arguments(Path.of(BASE_PATH, "7_1/cvdp-notifiedperson.json"), "laboratory_7_1_covid"),
        arguments(
            Path.of(BASE_PATH, "7_1/mytp-notifiedperson.json"), "laboratory_7_1_tuberculosis"),
        arguments(Path.of(BASE_PATH, "7_3/anonymous.json"), "laboratory_7_3_anonymous"),
        arguments(Path.of(BASE_PATH, "7_3/nonnominal-notifiedperson.json"), "laboratory_7_3"),
        arguments(Path.of(BASE_PATH, "7_3/nonnominal-notbyname.json"), "laboratory_7_3"),
        arguments(Path.of(BASE_PATH, "7_3/disease-nonnominal.json"), "disease_7_3"),
        arguments(
            Path.of(BASE_PATH, "7_3/disease-anonymous-nonnominal.json"), "disease_7_3_anonymous"),
        arguments(Path.of(BASE_PATH, "7_4/negative-covid19-bundle.json"), "laboratory_7_4"),
        arguments(Path.of(BASE_PATH, "invalid_71.json"), "laboratory_7_1"),
        arguments(
            Path.of(BASE_PATH, "invalid_71_multiple_observations_mixed.json"), "laboratory_7_1"),
        arguments(
            Path.of(BASE_PATH, "invalid_71_multiple_observations_all_neg.json"), "laboratory_7_1"));
  }

  @SpringBootTest(
      classes = NotificationRoutingApplication.class,
      useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
      webEnvironment = SpringBootTest.WebEnvironment.MOCK,
      properties = {
        "nrs.routing-rules=rules/routingConfig_73enabled.json",
        "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup",
        "nrs.rules-start-id=start",
        "nrs.checkWorkaroundIsWorking=false"
      })
  @Nested
  class For73 {
    @Autowired private RulesService rulesService;

    @Autowired private FhirContext fhirContext;

    @MethodSource("de.gematik.demis.nrs.rules.RulesServiceOtherIT#bundleToExpectedResultId")
    @ParameterizedTest
    void thatRuleMatches(final Path path, final String expectedRule) throws IOException {
      final String bundleJson = Files.readString(path);
      final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

      final Optional<Result> resultCandidate = rulesService.evaluateRules(bundle);
      assertThat(resultCandidate).isPresent();
      final Result result = resultCandidate.get();
      assertThat(result.id()).isEqualTo(expectedRule);
    }
  }

  @SpringBootTest(
      classes = NotificationRoutingApplication.class,
      useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
      webEnvironment = SpringBootTest.WebEnvironment.MOCK,
      properties = {
        "nrs.routing-rules=rules/routingConfig_with_follow_up.json",
        "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup",
        "nrs.rules-start-id=start",
        "nrs.checkWorkaroundIsWorking=false"
      })
  @Nested
  class FollowUp {
    @Autowired private RulesService rulesService;

    @Autowired private FhirContext fhirContext;

    @ParameterizedTest
    @MethodSource("de.gematik.demis.nrs.rules.RulesServiceOtherIT#bundleToExpectedResultId")
    void thatRuleMatches(final Path path, final String expectedRule) throws IOException {
      final String bundleJson = Files.readString(path);
      final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

      final Optional<Result> resultCandidate = rulesService.evaluateRules(bundle);
      assertThat(resultCandidate).isPresent();
      final Result result = resultCandidate.get();
      assertThat(result.id()).isEqualTo(expectedRule);
    }
  }
}
