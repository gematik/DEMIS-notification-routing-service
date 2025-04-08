package de.gematik.demis.nrs.rules;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = NotificationRoutingApplication.class,
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = {
      "nrs.routing-rules=rules/routingConfig.json",
      "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup",
      "nrs.rules-start-id=start"
    })
class RulesServiceIT {

  @Autowired private RulesService rulesService;

  @Autowired private FhirContext fhirContext;

  private static void assertResult(
      final Result result, final String category, final Set<RulesResultTypeEnum> receiver) {
    assertThat(result.notificationCategory()).isEqualTo(category);
    List<Route> resultRoutes = result.routesTo();
    assertThat(resultRoutes).hasSize(receiver.size());
    assertThat(resultRoutes).extracting("type").containsExactlyInAnyOrderElementsOf(receiver);
  }

  private static Stream<Arguments> p73Bundles() {
    return Stream.of(
        arguments(
            Path.of("src/test/resources/fhir/7_3/anonymous.json"),
            "laboratory_notification7_3_anonymous"),
        arguments(
            Path.of("src/test/resources/fhir/7_3/nonnominal-notifiedperson.json"),
            "laboratory_notification7_3"),
        arguments(
            Path.of("src/test/resources/fhir/7_3/nonnominal-notbyname.json"),
            "laboratory_notification7_3"));
  }

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/fhir/laboratory-notification-bundle.json,7.1",
    "src/test/resources/fhir/disease-notification-bundle.json,6.1",
  })
  void testEvaluateRules(final String path, final String category) throws IOException {
    // Load a sample FHIR bundle from a JSON file
    String bundleJson = Files.readString(Paths.get(path));
    Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    // Evaluate the rules
    Optional<Result> optionalResult = rulesService.evaluateRules(bundle);

    // Assert the expected result
    assertThat(optionalResult).isPresent();
    final Result result = optionalResult.get();
    assertResult(
        result,
        category,
        Set.of(
            RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
            RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
            RulesResultTypeEnum.SPECIFIC_RECEIVER));
  }

  @Test
  void that74IsProcessed() throws IOException {
    final String bundleJson =
        Files.readString(Path.of("src/test/resources/fhir/7_4/negative-covid19-bundle.json"));
    final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    // Evaluate the rules
    final Optional<Result> optionalResult = rulesService.evaluateRules(bundle);

    // Assert the expected result
    assertThat(optionalResult).isPresent();
    final Result result = optionalResult.get();
    assertThat(result.id()).isEqualTo("notification7_4");
    assertResult(result, "7.4", Set.of(RulesResultTypeEnum.SPECIFIC_RECEIVER));
  }

  @ParameterizedTest
  @MethodSource("p73Bundles")
  void that73IsProcessed(final Path path, final String expectedResultId) throws IOException {
    // Load a sample FHIR bundle from a JSON file
    final String bundleJson = Files.readString(path);
    final Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    // Evaluate the rules
    final Optional<Result> optionalResult = rulesService.evaluateRules(bundle);

    // Assert the expected optionalResult
    assertThat(optionalResult).isPresent();
    final Result result = optionalResult.get();
    assertThat(result.id()).isEqualTo(expectedResultId);
    assertResult(result, "7.3", Set.of(RulesResultTypeEnum.SPECIFIC_RECEIVER));
  }
}
