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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import static de.gematik.demis.nrs.service.fhir.DemisFhirConstants.EXTENSION_URL_FACILITY_ADDRESS_NOTIFIED_PERSON;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BinaryOperator;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.BaseReference;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class AddressResolver {

  private static final BinaryOperator<Address> TAKE_FIRST_AND_LOG =
      (a, b) -> {
        log.info("Multiple addresses for the same use. The first one is taken");
        return a;
      };

  private static <T> Optional<T> first(final List<T> list) {
    if (list == null || list.isEmpty()) {
      return Optional.empty();
    } else {
      if (list.size() > 1) {
        log.info("More than one address. The first one is taken and the others are discarded");
      }
      return Optional.of(list.get(0));
    }
  }

  // TODO gelb! why???
  public Optional<Address> fromPractitionerRole(final PractitionerRole role) {
    if (role.hasOrganization()
        && (role.getOrganization().getResource() instanceof Organization organization)) {
      return fromOrganization(organization);
    }
    if (role.hasPractitioner()
        && (role.getPractitioner().getResource() instanceof Practitioner practitioner)) {
      return fromPractitioner(practitioner);
    }
    // throw 422? -> nö
    return Optional.empty();
  }

  private Optional<Address> fromOrganization(final Organization organization) {
    return first(organization.getAddress());
  }

  private Optional<Address> fromPractitioner(final Practitioner practitioner) {
    return first(practitioner.getAddress());
  }

  public Map<AddressUseEnum, Address> fromPatient(final Patient patient) {
    return patient.getAddress().stream()
        .collect(
            toMap(AddressUseEnum::fromAddress, this::preferFacilityAddress, TAKE_FIRST_AND_LOG));
  }

  private Address preferFacilityAddress(final Address address) {
    final Extension extension =
        address.getExtensionByUrl(EXTENSION_URL_FACILITY_ADDRESS_NOTIFIED_PERSON);
    if (extension != null
        && (extension.getValue() instanceof final BaseReference baseReference
            && (baseReference.getResource()
                instanceof final Organization notifiedPersonFacility))) {
      return fromOrganization(notifiedPersonFacility).orElse(address);
    }
    return address;
  }
}
