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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.disease.NotificationDiseaseDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.LaboratoryReportDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NonNominalBundleBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.laboratory.NotificationLaboratoryDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.utils.DemisConstants;
import de.gematik.demis.nrs.NotificationRoutingApplication;
import de.gematik.demis.nrs.rules.model.Rule;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DiagnosticReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = NotificationRoutingApplication.class,
    useMainMethod = SpringBootTest.UseMainMethod.ALWAYS,
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup")
public class RulesTest {
  @Autowired private RulesConfig config;

  @Autowired private FhirContext fhirContext;

  private IFhirPath fhirPath;

  private static Bundle get73LaboratoryBundle(final String pathogenCode) {
    final DiagnosticReport diagnosticReport =
        new LaboratoryReportDataBuilder()
            .setDefaultData()
            .setCodeCode(pathogenCode)
            .setCodeSystem(DemisConstants.CODE_SYSTEM_NOTIFICATION_CATEGORY)
            .setCodeDisplay("")
            .build();
    final Composition laboratoryReport =
        new NotificationLaboratoryDataBuilder()
            .setDefault()
            .setLaboratoryReport(diagnosticReport)
            .build();
    return new NonNominalBundleBuilder()
        .setDefaults()
        .setLaboratoryReport(diagnosticReport)
        .setNotificationLaboratory(laboratoryReport)
        .build();
  }

  private static Stream<String> pathogenCodes73Laboratory() {
    return Stream.of("chtp", "echp", "hivp", "negp", "toxp", "trpp");
  }

  private static Stream<String> pathogenCodes73Disease() {
    return Stream.of("chtd", "echd", "hivd", "negd", "toxd", "trpd");
  }

  @BeforeEach
  void setup() {
    fhirPath = fhirContext.newFhirPath();
  }

  @ParameterizedTest
  @MethodSource("pathogenCodes73Laboratory")
  void that73PathogenLaboratoryCodesAreMatched(final String pathogenCode) {
    final Bundle build = get73LaboratoryBundle(pathogenCode);

    final CustomEvaluationContext evaluationContext = new CustomEvaluationContext(build);
    fhirPath.setEvaluationContext(evaluationContext);

    final Map<String, Rule> rules = config.rules();
    final Rule laboratory = rules.get("laboratory");
    Objects.requireNonNull(laboratory);

    final boolean isMatching = laboratory.checkRule(fhirPath, build);
    assertThat(isMatching).isTrue();
  }

  @ParameterizedTest
  @MethodSource("pathogenCodes73Disease")
  void that73PathogenDiseaseCodesAreMatched(final String pathogenCode) {
    final Condition disease =
        new Condition()
            .setCode(
                new CodeableConcept()
                    .setCoding(
                        List.of(
                            new Coding(
                                DemisConstants.CODE_SYSTEM_NOTIFICATION_CATEGORY,
                                pathogenCode,
                                ""))));
    final Composition diagnosticReport =
        new NotificationDiseaseDataBuilder().setDefaults().setDisease(disease).build();
    final Bundle build =
        new NonNominalBundleBuilder()
            .setDefaults()
            .setNotificationLaboratory(diagnosticReport)
            .addAdditionalEntry(disease)
            .build();

    final CustomEvaluationContext evaluationContext = new CustomEvaluationContext(build);
    fhirPath.setEvaluationContext(evaluationContext);

    final Map<String, Rule> rules = config.rules();
    final Rule rule = rules.get("disease");
    Objects.requireNonNull(rule);

    final boolean isMatching = rule.checkRule(fhirPath, build);
    assertThat(isMatching).isTrue();
  }
}
