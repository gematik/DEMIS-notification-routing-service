package de.gematik.demis.nrs.api;

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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.nrs.service.ExceptionMessages;
import de.gematik.demis.service.base.error.ServiceCallException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class FhirRestExceptionHandlerTest {

  private final FhirRestExceptionHandler handler = new FhirRestExceptionHandler();

  @Test
  void handleServerException_shouldReturnServiceUnavailable_onConnectException() {
    Exception ex = new Exception(new ConnectException("Connection failed"));
    ResponseEntity<Object> response = handler.handleServerException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  @Test
  void handleServerException_shouldReturnGatewayTimeout_onSocketTimeoutException() {
    Exception ex = new Exception(new SocketTimeoutException("Timeout"));
    ResponseEntity<Object> response = handler.handleServerException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  @Test
  void handleServerException_shouldReturnInternalServerError_onOtherException() {
    Exception ex = new Exception("Other error");
    ResponseEntity<Object> response = handler.handleServerException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  @Test
  void handleFeignException_shouldReturnUnprocessableEntity_onNotFound() {
    ServiceCallException ex = new ServiceCallException("Not found", "id", 404, null);
    ResponseEntity<Object> response = handler.handleFeignException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.UNPROCESSABLE_ENTITY_DLR);
  }

  @Test
  void handleFeignException_shouldReturnBadGateway_on5xx() {
    ServiceCallException ex = new ServiceCallException("Server error", "id", 502, null);
    ResponseEntity<Object> response = handler.handleFeignException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  @Test
  void handleFeignException_shouldReturnInternalServerError_onOtherStatus() {
    ServiceCallException ex = new ServiceCallException("Other error", "id", 400, null);
    ResponseEntity<Object> response = handler.handleFeignException(ex);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isEqualTo(ExceptionMessages.INTERNAL_SERVER_ERROR);
  }
}
