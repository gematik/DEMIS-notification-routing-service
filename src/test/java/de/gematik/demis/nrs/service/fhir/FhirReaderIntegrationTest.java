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

import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_CURRENT;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_ORDINARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_OTHER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.SUBMITTER;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.config.FhirConfiguration;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.DestinationLookupReaderInput;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.nrs.test.FileUtil;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Optional;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {
      FhirReader.class,
      AddressMapper.class,
      AddressResolver.class,
      FhirConfiguration.class
    })
class FhirReaderIntegrationTest {

  private static final String LABORATORY_NOTIFICATION =
      "/fhir/7_1/cvdp-notifiedperson-parameters.json";
  // This file is located in /src/main/resources instead of /src/test/resources and is used to warm
  // up the FHIRParser
  private static final String DISEASE_NOTIFICATION = "/fhir/disease-notification.json";
  private static final String FOLLOW_UP_NOTIFICATION =
      "/fhir/7_1/anonymous-followup-notification.json";

  @Autowired FhirReader underTest;

  @Test
  void extractRoutingInput_Laboratory() {
    final String fhirAsJson = FileUtil.readResource(LABORATORY_NOTIFICATION);
    final RoutingInput routingInput = underTest.extractRoutingInput(fhirAsJson);
    assertThat(routingInput)
        .isNotNull()
        .extracting(
            RoutingInput::addresses,
            InstanceOfAssertFactories.map(AddressOriginEnum.class, AddressDTO.class))
        .containsOnly(
            entry(
                NOTIFIED_PERSON_PRIMARY,
                new AddressDTO("Primarystr.", "123", "44801", "Bochum", "20422")),
            entry(
                NOTIFIED_PERSON_CURRENT,
                new AddressDTO("Currentstraße", "12a", "51427", "Bergisch Gladbach", "20422")),
            entry(
                NOTIFIED_PERSON_ORDINARY,
                new AddressDTO("Dingsweg", "321", "13055", "Berlin", "20422")),
            entry(
                NOTIFIED_PERSON_OTHER,
                new AddressDTO("Anderestr.", "98z", "21482", "Berlin", null)),
            entry(SUBMITTER, new AddressDTO("Teststr.", "123", "13055", "Berlin", "20422")),
            entry(NOTIFIER, new AddressDTO("Dingsweg", "321", "13055", "Berlin", "20422")));
  }

  @Test
  void extractRoutingInput_Disease() {
    final String fhirAsJson = FileUtil.readResource(DISEASE_NOTIFICATION);
    final RoutingInput routingInput = underTest.extractRoutingInput(fhirAsJson);
    assertThat(routingInput)
        .isNotNull()
        .extracting(
            RoutingInput::addresses,
            InstanceOfAssertFactories.map(AddressOriginEnum.class, AddressDTO.class))
        .containsOnly(
            entry(
                NOTIFIED_PERSON_PRIMARY,
                new AddressDTO("Berthastraße", "123", "12345", "Betroffenenstadt", "20422")),
            entry(
                NOTIFIED_PERSON_CURRENT,
                new AddressDTO("Krankenhausstraße", "1", "21481", "Buchhorst", "20422")),
            entry(
                NOTIFIER, new AddressDTO("Krankenhausstraße", "1", "21481", "Buchhorst", "20422")));
  }

  @Test
  void extractRoutingInput_InvalidNotification() {
    org.junit.jupiter.api.Assertions.assertThrows(
        ServiceException.class, () -> underTest.extractRoutingInput("{}"));
  }

  @Test
  void validDestinationLookupReaderInformation() {
    final DestinationLookupReaderInput expected =
        new DestinationLookupReaderInput("5001b5e1-a94c-4d7c-a35d-5d62fe491196", "denp");
    Optional<DestinationLookupReaderInput> destinationLookupReaderInformation =
        underTest.getDestinationLookupReaderInformation(
            underTest.toBundle(FileUtil.readResource(FOLLOW_UP_NOTIFICATION)));
    assertThat(destinationLookupReaderInformation).contains(expected);
  }

  @Test
  void invalidDestinationLookupReaderInformationOtherRelatesTo() {
    Optional<DestinationLookupReaderInput> destinationLookupReaderInformation =
        underTest.getDestinationLookupReaderInformation(
            underTest.toBundle(FileUtil.readResource(DISEASE_NOTIFICATION)));
    assertThat(destinationLookupReaderInformation).isEmpty();
  }

  @Test
  void invalidDestinationLookupReaderInformationNoRelatesTo() {
    Optional<DestinationLookupReaderInput> destinationLookupReaderInformation =
        underTest.getDestinationLookupReaderInformation(
            underTest.toBundle(FileUtil.readResource(LABORATORY_NOTIFICATION)));
    assertThat(destinationLookupReaderInformation).isEmpty();
  }
}
