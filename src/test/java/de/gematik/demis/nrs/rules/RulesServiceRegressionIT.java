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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.uhn.fhir.context.FhirContext;
import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
      "nrs.routing-rules=src/test/resources/routingConfigNotExisting.json",
      "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup"
    })
class RulesServiceRegressionIT {

  @Autowired private RulesService rulesService;

  @Autowired private RulesConfig rulesConfig;

  @Autowired private FhirContext fhirContext;

  @ParameterizedTest
  @CsvSource({
    "src/test/resources/fhir/laboratory-notification-bundle.json,notification7_1",
    "src/test/resources/fhir/disease-notification-bundle.json,notification6_1",
  })
  void testEvaluateRules(String path, String resultCase) throws IOException {
    // Load a sample FHIR bundle from a JSON file
    String bundleJson = Files.readString(Paths.get(path));
    Bundle bundle = (Bundle) fhirContext.newJsonParser().parseResource(bundleJson);

    assertThatThrownBy(() -> rulesService.evaluateRules(bundle))
        .isInstanceOf(NullPointerException.class)
        .hasMessage(
            "Cannot invoke \"de.gematik.demis.nrs.rules.model.Rule.checkRule(ca.uhn.fhir.fhirpath.IFhirPath, org.hl7.fhir.r4.model.Bundle)\" because \"rule\" is null");
  }
}
