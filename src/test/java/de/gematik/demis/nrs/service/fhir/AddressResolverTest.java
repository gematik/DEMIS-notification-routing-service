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

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import de.gematik.demis.notification.builder.demis.fhir.notification.builder.infectious.NotifiedPersonDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.AddressDataBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.OrganizationBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerBuilder;
import de.gematik.demis.notification.builder.demis.fhir.notification.builder.technicals.PractitionerRoleBuilder;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.junit.jupiter.api.Test;

class AddressResolverTest {

  private final AddressResolver underTest = new AddressResolver();

  @Test
  void fromPractitionerRole_Orga() {
    final Address orgaAddress = createAddress();
    final Organization orga = new OrganizationBuilder().setAddress(orgaAddress).build();
    final PractitionerRole role = new PractitionerRoleBuilder().withOrganization(orga).build();

    final Optional<Address> result = underTest.fromPractitionerRole(role);

    assertThat(result).hasValue(orgaAddress);
  }

  @Test
  void fromPractitionerRole_Person() {
    final Address address = createAddress();
    final Practitioner practitioner = new PractitionerBuilder().setAddress(address).build();
    final PractitionerRole role =
        new PractitionerRoleBuilder().withPractitioner(practitioner).build();

    final Optional<Address> result = underTest.fromPractitionerRole(role);

    assertThat(result).hasValue(address);
  }

  @Test
  void fromPractitionerRole_NoReference() {
    final PractitionerRole role = new PractitionerRoleBuilder().build();

    final Optional<Address> result = underTest.fromPractitionerRole(role);

    assertThat(result).isNotPresent();
  }

  @Test
  void fromPatient() {
    final Address address = createAddress();
    final Patient patient = new NotifiedPersonDataBuilder().addAddress(address).build();

    final Map<AddressUseEnum, Address> result = underTest.fromPatient(patient);
    assertThat(result).containsExactly(entry(AddressUseEnum.OTHER, address));
  }

  @Test
  void fromPatient_preferFacilityAddress() {
    final Address orgaAddress = createAddress();
    final Organization orga = new OrganizationBuilder().setAddress(orgaAddress).build();
    final Patient patient =
        new NotifiedPersonDataBuilder()
            .addAddress(
                new AddressDataBuilder()
                    .withOrganizationReferenceExtension(orga)
                    .setCity("Ignored")
                    .setCountry("XX")
                    .setPostalCode("99999")
                    .setStreet("Ignoredstr.")
                    .build())
            .build();

    final Map<AddressUseEnum, Address> result = underTest.fromPatient(patient);
    assertThat(result).containsExactly(entry(AddressUseEnum.OTHER, orgaAddress));
  }

  @Test
  void fromPatient_multipleAddresses() {
    final Address primaryAddress =
        new AddressDataBuilder().setCity("Berlin").withAddressUseExtension("primary", null).build();

    final Address ignoredSecondPrimaryAddress =
        new AddressDataBuilder()
            .setCity("Hamburg")
            .withAddressUseExtension("primary", null)
            .build();

    final Address currentAddress =
        new AddressDataBuilder().setCity("Bochum").withAddressUseExtension("current", null).build();

    final Patient patient =
        new NotifiedPersonDataBuilder()
            .addAddress(primaryAddress)
            .addAddress(ignoredSecondPrimaryAddress)
            .addAddress(currentAddress)
            .build();

    final Map<AddressUseEnum, Address> result = underTest.fromPatient(patient);
    assertThat(result)
        .containsOnly(
            entry(AddressUseEnum.PRIMARY, primaryAddress),
            entry(AddressUseEnum.CURRENT, currentAddress));
  }

  private static Address createAddress() {
    return new Address()
        .setCountry("DEU")
        .setPostalCode("12345")
        .setCity("Bochum")
        .addLine("Meinestr. 5");
  }
}
