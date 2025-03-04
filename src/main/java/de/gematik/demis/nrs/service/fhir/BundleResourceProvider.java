package de.gematik.demis.nrs.service.fhir;

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

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Composition;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;

@RequiredArgsConstructor
class BundleResourceProvider {

  private final Bundle bundle;

  public String getIdentifier() {
    return Optional.ofNullable(bundle.getIdentifier()).map(Identifier::getValue).orElse(null);
  }

  public Optional<Patient> getNotifiedPerson() {
    return getComposition().getSubject().getResource() instanceof Patient notifiedPerson
        ? Optional.of(notifiedPerson)
        : Optional.empty();
  }

  public Optional<PractitionerRole> getNotifierRole() {
    return getComposition().getAuthorFirstRep().getResource()
            instanceof PractitionerRole notifierRole
        ? Optional.of(notifierRole)
        : Optional.empty();
  }

  public Optional<PractitionerRole> getSubmittingRole() {
    return findResource(PractitionerRole.class, DemisFhirConstants.SUBMITTING_ROLE_PROFILE);
  }

  private Composition getComposition() {
    return (Composition) bundle.getEntryFirstRep().getResource();
  }

  private <T extends Resource> Optional<T> findResource(
      final Class<T> clazz, final String profile) {
    return bundle.getEntry().stream()
        .map(Bundle.BundleEntryComponent::getResource)
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .filter(resource -> hasProfile(resource, profile))
        .findFirst();
  }

  private static boolean hasProfile(final Resource resource, final String profileUrl) {
    final boolean metaMatch =
        resource.getMeta().getProfile().stream()
            .map(PrimitiveType::getValue)
            .anyMatch(profileUrl::equals);
    if (metaMatch) {
      return true;
    }
    final ResourceDef resDef = resource.getClass().getAnnotation(ResourceDef.class);
    return resDef != null && profileUrl.equals(resDef.profile());
  }

  /*
      Fhir Notes:

      Notifier und Submitter
      Type: PractitionerRole
      Role: NotifierRole oder SubmittingRole

  NotifierFacility

  Submitter NUR bei Labor!!!!
       */
}
