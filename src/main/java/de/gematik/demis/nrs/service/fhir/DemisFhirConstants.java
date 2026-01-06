package de.gematik.demis.nrs.service.fhir;

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

final class DemisFhirConstants {

  public static final String PROFILE_BASE_URL = "https://demis.rki.de/fhir/";

  public static final String EXTENSION_URL_FACILITY_ADDRESS_NOTIFIED_PERSON =
      PROFILE_BASE_URL + "StructureDefinition/FacilityAddressNotifiedPerson";

  public static final String ADDRESS_USE_EXTENSION =
      PROFILE_BASE_URL + "StructureDefinition/AddressUse";

  public static final String ADDRESS_USE_SYSTEM = PROFILE_BASE_URL + "CodeSystem/addressUse";

  public static final String SUBMITTING_ROLE_PROFILE =
      PROFILE_BASE_URL + "StructureDefinition/SubmittingRole";

  private DemisFhirConstants() {
    // no instances
  }
}
