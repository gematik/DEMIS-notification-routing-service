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

import ca.uhn.fhir.fhirpath.IFhirPathEvaluationContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;

public class CustomEvaluationContext implements IFhirPathEvaluationContext {

  private final Bundle bundle;

  public CustomEvaluationContext(Bundle bundle) {
    this.bundle = bundle;
  }

  @Override
  public IBase resolveReference(@Nonnull IIdType theReference, @Nullable IBase theContext) {
    String referenceValue = theReference.getValue(); // Full reference value
    String referenceIdPart = theReference.getIdPart(); // Just the ID part (e.g., "123456")

    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      // Check if the fullUrl matches
      if (referenceValue.equals(entry.getFullUrl())) {
        return entry.getResource();
      }

      // Check if the resource type and ID match
      if (entry.getResource().getIdElement().getIdPart().equals(referenceIdPart)
          && entry
              .getResource()
              .getResourceType()
              .toString()
              .equals(theReference.getResourceType())) {
        return entry.getResource();
      }
    }

    throw new UnsupportedOperationException(
        "Reference resolution not supported for: " + referenceValue);
  }
}
