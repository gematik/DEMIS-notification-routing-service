package de.gematik.demis.nrs.rules.model;

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

import ca.uhn.fhir.fhirpath.IFhirPath;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Bundle;

public record Rule(
    String id,
    String description,
    String fhirPathExpression,
    Map<String, String> result,
    Map<String, String> followingRules) {

  /** Copy the data from original but set the id to newId. */
  public static Rule replaceRuleId(final Rule original, final String newId) {
    return new Rule(
        newId,
        original.description(),
        original.fhirPathExpression(),
        original.result(),
        original.followingRules());
  }

  public boolean checkRule(IFhirPath fhirPath, Bundle bundle) {
    List<IBase> result = fhirPath.evaluate(bundle, fhirPathExpression, IBase.class);
    return !result.isEmpty() && ((BooleanType) result.get(0)).booleanValue();
  }

  public boolean resultExists() {
    return result != null && !result.isEmpty();
  }
}
