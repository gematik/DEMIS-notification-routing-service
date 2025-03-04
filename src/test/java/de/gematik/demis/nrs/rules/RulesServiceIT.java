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
 * #L%
 */

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/fhir/laboratory-notification-bundle.json,notification6_1_7_1",
    "src/test/resources/fhir/disease-notification-bundle.json,notification6_1_7_1",
  })
  void testEvaluateRules(String path) throws IOException {
    // Load a sample FHIR bundle from a JSON file
    String bundleJson = Files.readString(Paths.get(path));
    Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    // Evaluate the rules
    Optional<Result> result = rulesService.evaluateRules(bundle);

    // Assert the expected result
    assertThat(result).isPresent();
    List<Route> resultRoutes = result.get().routesTo();
    assertThat(resultRoutes).hasSize(3);
    assertThat(resultRoutes)
        .extracting("type")
        .containsExactly(
            RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE,
            RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS,
            RulesResultTypeEnum.SPECIFIC_RECEIVER);
  }
}
