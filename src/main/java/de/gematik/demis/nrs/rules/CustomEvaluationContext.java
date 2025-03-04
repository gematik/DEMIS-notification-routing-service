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
    for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
      if (entry.getResource().getIdElement().getIdPart().equals(theReference.getIdPart())) {
        return entry.getResource();
      }
    }

    throw new UnsupportedOperationException(
        "Reference resolution not supported for: " + theReference.getValue());
  }
}
