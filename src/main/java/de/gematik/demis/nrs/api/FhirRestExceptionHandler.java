package de.gematik.demis.nrs.api;

/*-
 * #%L
 * notification-processing-service
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

import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import de.gematik.demis.nrs.service.ExceptionMessages;
import de.gematik.demis.service.base.error.ServiceCallException;
import de.gematik.demis.service.base.error.ServiceException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class FhirRestExceptionHandler extends ResponseEntityExceptionHandler {

  private static boolean hasCause(final Exception ex, final Class<? extends Throwable> clazz) {
    Throwable cause = ex;
    while ((cause = cause.getCause()) != null && cause != ex) {
      if (clazz.isInstance(cause)) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(Exception.class)
  public final ResponseEntity<Object> handleServerException(final Exception ex) {
    final HttpStatus responseStatus;
    if (hasCause(ex, ConnectException.class)) {
      responseStatus = HttpStatus.SERVICE_UNAVAILABLE;
    } else if (hasCause(ex, SocketTimeoutException.class)) {
      responseStatus = HttpStatus.GATEWAY_TIMEOUT;
    } else {
      responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return handleError(responseStatus, ex, ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(ServiceException.class)
  public final ResponseEntity<Object> handleServiceException(final ServiceException ex) {
    final HttpStatus responseStatus =
        Optional.ofNullable(ex.getResponseStatus()).orElse(HttpStatus.INTERNAL_SERVER_ERROR);
    return handleError(responseStatus, ex, ex.getMessage());
  }

  @ExceptionHandler(ServiceCallException.class)
  public final ResponseEntity<Object> handleFeignException(final ServiceCallException ex) {
    final HttpStatus resolvedStatusCode = HttpStatus.resolve(ex.getHttpStatus());
    if (resolvedStatusCode == HttpStatus.NOT_FOUND) {
      return handleError(UNPROCESSABLE_ENTITY, ex, ExceptionMessages.UNPROCESSABLE_ENTITY_DLR);
    }
    final HttpStatus responseStatus =
        resolvedStatusCode != null && resolvedStatusCode.is5xxServerError()
            ? HttpStatus.BAD_GATEWAY
            : HttpStatus.INTERNAL_SERVER_ERROR;
    return handleError(responseStatus, ex, ExceptionMessages.INTERNAL_SERVER_ERROR);
  }

  private ResponseEntity<Object> handleError(
      final HttpStatusCode statusCode, final Exception ex, final String body) {
    logException(statusCode, ex);
    return ResponseEntity.status(statusCode).contentType(MediaType.TEXT_PLAIN).body(body);
  }

  private void logException(final HttpStatusCode statusCode, final Exception ex) {
    if (statusCode.is5xxServerError()) {
      log.error("server error processing request", ex);
    } else {
      log.info("invalid client request: {}", String.valueOf(ex));
    }
  }
}
