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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomEvaluationContextTest {

  private Bundle bundle;
  private CustomEvaluationContext evaluationContext;

  @BeforeEach
  void setUp() {
    // Initialize a new Bundle
    bundle = new Bundle();

    // Add a Patient resource with fullUrl and ID
    Patient patient = new Patient();
    patient.setId("123456");
    patient.setActive(true);
    bundle.addEntry().setFullUrl("urn:uuid:123456").setResource(patient);

    // Add a Laboratory Report (Observation) resource with fullUrl and ID
    Observation labReport = new Observation();
    labReport.setId("789012");
    labReport.setStatus(Observation.ObservationStatus.FINAL);
    labReport.setCode(new CodeableConcept().setText("Laboratory Report"));
    labReport.setSubject(new Reference("urn:uuid:123456")); // Reference the Patient
    bundle
        .addEntry()
        .setFullUrl("https://example.com/fhir/Observation/789012")
        .setResource(labReport);

    // Add a Composition resource that references both the Patient and the Laboratory Report
    Composition composition = new Composition();
    composition.setId("composition1");
    composition.setTitle("Medical Summary");
    composition.setStatus(Composition.CompositionStatus.FINAL);

    // Reference the Patient with a short URL
    composition.addAuthor(new Reference("Patient/123456"));

    // Reference the Laboratory Report with a full URL
    composition.addSection().addEntry(new Reference("https://example.com/fhir/Observation/789012"));

    bundle.addEntry().setFullUrl("urn:uuid:composition1").setResource(composition);

    // Initialize the CustomEvaluationContext with the bundle
    evaluationContext = new CustomEvaluationContext(bundle);
  }

  @Test
  void resolveReference_shouldResolvePatientFromCompositionSubject() {
    // Given a reference to the Patient via the Composition's subject field
    IIdType reference = new IdType("Patient", "123456");

    // When resolving the reference
    IBase resolvedResource = evaluationContext.resolveReference(reference, null);

    // Then it should resolve to the Patient resource
    assertThat(resolvedResource).isInstanceOf(Patient.class);
    assertThat(((Patient) resolvedResource).getIdElement().getIdPart()).isEqualTo("123456");
    assertThat(((Patient) resolvedResource).getActive()).isTrue();
  }

  @Test
  void resolveReference_shouldResolveLaboratoryReportFromCompositionSection() {
    // Given a reference to the Laboratory Report via the Composition's section entry
    IIdType reference = new IdType("https://example.com/fhir/Observation/789012");

    // When resolving the reference
    IBase resolvedResource = evaluationContext.resolveReference(reference, null);

    // Then it should resolve to the Laboratory Report (Observation) resource
    assertThat(resolvedResource).isInstanceOf(Observation.class);
    assertThat(((Observation) resolvedResource).getIdElement().getIdPart()).isEqualTo("789012");
    assertThat(((Observation) resolvedResource).getStatus())
        .isEqualTo(Observation.ObservationStatus.FINAL);
    assertThat(((Observation) resolvedResource).getCode().getText()).isEqualTo("Laboratory Report");
  }

  @Test
  void resolveReference_shouldResolveCompositionUsingFullUrl() {
    // Given a reference to the Composition's fullUrl
    IIdType reference = new IdType("urn:uuid:composition1");

    // When resolving the reference
    IBase resolvedResource = evaluationContext.resolveReference(reference, null);

    // Then it should resolve to the Composition resource
    assertThat(resolvedResource).isInstanceOf(Composition.class);
    assertThat(((Composition) resolvedResource).getIdElement().getIdPart())
        .isEqualTo("composition1");
    assertThat(((Composition) resolvedResource).getTitle()).isEqualTo("Medical Summary");
    assertThat(((Composition) resolvedResource).getStatus())
        .isEqualTo(Composition.CompositionStatus.FINAL);
  }

  @Test
  void resolveReference_shouldThrowExceptionForUnknownCompositionReference() {
    // Given a reference to an unknown Composition
    IIdType reference = new IdType("urn:uuid:unknown-composition");

    // When resolving the reference
    // Then it should throw an UnsupportedOperationException
    assertThatThrownBy(() -> evaluationContext.resolveReference(reference, null))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining(
            "Reference resolution not supported for: urn:uuid:unknown-composition");
  }

  @Test
  void resolveReference_shouldThrowExceptionForUnknownPatientReferenceInComposition() {
    // Given a reference to an unknown Patient in the Composition
    IIdType reference = new IdType("Patient", "unknown");

    // When resolving the reference
    // Then it should throw an UnsupportedOperationException
    assertThatThrownBy(() -> evaluationContext.resolveReference(reference, null))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Reference resolution not supported for: Patient/unknown");
  }
}
