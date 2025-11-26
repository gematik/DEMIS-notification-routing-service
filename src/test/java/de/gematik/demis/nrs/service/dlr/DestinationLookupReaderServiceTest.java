package de.gematik.demis.nrs.service.dlr;

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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.service.dto.DestinationLookupReaderInput;
import de.gematik.demis.nrs.service.dto.DestinationLookupReaderResponse;
import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.service.base.error.ServiceCallException;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DestinationLookupReaderServiceTest {

  private DestinationLookupReaderClient destinationLookupReaderClient;
  private DestinationLookupReaderService destinationLookupReaderService;
  private FhirReader fhirReader;

  @BeforeEach
  void setUp() {
    destinationLookupReaderClient = mock(DestinationLookupReaderClient.class);
    fhirReader = mock(FhirReader.class);
    destinationLookupReaderService =
        new DestinationLookupReaderService(destinationLookupReaderClient, fhirReader);
  }

  @Test
  void getDepartmentFromDlr_success() {
    final String responseBody = "myDepartment";
    when(fhirReader.getDestinationLookupReaderInformation(any(), any()))
        .thenReturn(
            Optional.of(
                new DestinationLookupReaderInput("3bc0a462-5088-4f04-b94a-f9b2d3433cfe", "cat")));
    when(destinationLookupReaderClient.getDepartment(any()))
        .thenReturn(new DestinationLookupReaderResponse(responseBody));
    Optional<String> department =
        destinationLookupReaderService.getDepartmentForFollowUpNotification(
            new Bundle(), "pathogen");
    assertThat(department).contains(responseBody);
  }

  @Test
  void getDepartmentFromDlr_notFound() {
    when(fhirReader.getDestinationLookupReaderInformation(any(), any()))
        .thenReturn(
            Optional.of(
                new DestinationLookupReaderInput("3bc0a462-5088-4f04-b94a-f9b2d3433cfe", "cat")));
    when(destinationLookupReaderClient.getDepartment(any()))
        .thenThrow(new ServiceCallException("Not found", "404", 404, new Throwable()));

    assertThatThrownBy(
            () ->
                destinationLookupReaderService.getDepartmentForFollowUpNotification(
                    new Bundle(), "pathogen"))
        .isInstanceOf(ServiceCallException.class)
        .hasMessageContaining("Not found");
  }

  @Test
  void getDepartmentFromDlr_error() {
    when(fhirReader.getDestinationLookupReaderInformation(any(), any()))
        .thenReturn(
            Optional.of(
                new DestinationLookupReaderInput("3bc0a462-5088-4f04-b94a-f9b2d3433cfe", "cat")));
    when(destinationLookupReaderClient.getDepartment(any()))
        .thenThrow(
            new ServiceCallException("service response: error", "123", 500, new Throwable()));

    assertThatThrownBy(
            () ->
                destinationLookupReaderService.getDepartmentForFollowUpNotification(
                    new Bundle(), "pathogen"))
        .isInstanceOf(ServiceCallException.class)
        .hasMessageContaining("service response: error");
  }

  @Test
  void getDepartmentFromDlr_invalidUUID() {
    when(fhirReader.getDestinationLookupReaderInformation(any(), any()))
        .thenReturn(Optional.of(new DestinationLookupReaderInput("12345", "cat")));

    assertThatThrownBy(
            () ->
                destinationLookupReaderService.getDepartmentForFollowUpNotification(
                    new Bundle(), "pathogen"))
        .isInstanceOf(ServiceException.class)
        .hasMessageContaining(
            "NRS-003: no responsible health department found. NotificationId is not a valid UUID: 12345");
  }
}
