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

import static de.gematik.demis.nrs.service.ExceptionMessages.NO_HEALTH_OFFICE_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import de.gematik.demis.nrs.service.fhir.FhirReader;
import de.gematik.demis.nrs.util.UUIDValidator;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DestinationLookupReaderService {

  private final DestinationLookupReaderClient destinationLookupReaderClient;
  private final FhirReader fhirReader;

  public Optional<String> getDepartmentForFollowUpNotification(
      Bundle bundle, String notificationType) {
    log.info("RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO rule active. Calling DLR Service");
    return fhirReader
        .getDestinationLookupReaderInformation(bundle, notificationType)
        .map(
            input -> {
              if (!UUIDValidator.isValidUUID(input.notificationId())) {
                throw new ServiceException(
                    UNPROCESSABLE_ENTITY,
                    UNPROCESSABLE_ENTITY.getReasonPhrase(),
                    NO_HEALTH_OFFICE_FOUND
                        + " NotificationId is not a valid UUID: "
                        + input.notificationId());
              }
              return destinationLookupReaderClient
                  .getDepartment(input.notificationId())
                  .department();
            });
  }
}
