package de.gematik.demis.nrs.api;

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

import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Transform the RuleBasedRouteDTO to a Map and add fields based on enabled feature flags. This
 * approach scales better with many feature flags than Jackson JSON Views.
 */
@ControllerAdvice
public class RuleBasedRouteDTOAdvice implements ResponseBodyAdvice<Object> {

  private final boolean isPermissionCheckEnabled;
  private final boolean isCustodianEnabled;

  public RuleBasedRouteDTOAdvice(
      @Value("${feature.flag.permission.check.enabled}") final boolean isPermissionCheckEnabled,
      @Value("${feature.flag.custodian.enabled}") final boolean isCustodianEnabled) {
    this.isPermissionCheckEnabled = isPermissionCheckEnabled;
    this.isCustodianEnabled = isCustodianEnabled;
  }

  @Override
  public boolean supports(
      MethodParameter returnType, @Nonnull Class<? extends HttpMessageConverter<?>> converterType) {
    return RuleBasedRouteDTO.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  public Object beforeBodyWrite(
      @CheckForNull final Object body,
      @Nonnull final MethodParameter returnType,
      @Nonnull final MediaType selectedContentType,
      @Nonnull final Class<? extends HttpMessageConverter<?>> selectedConverterType,
      @Nonnull final ServerHttpRequest request,
      @Nonnull final ServerHttpResponse response) {
    if (body instanceof RuleBasedRouteDTO ruleBasedRouteDTO) {
      final Map<String, Object> result = new HashMap<>(ruleBasedRouteDTO.toMap());
      if (isPermissionCheckEnabled) {
        result.put(RuleBasedRouteDTO.ALLOWED_ROLES_KEY, ruleBasedRouteDTO.allowedRoles());
      }
      if (isCustodianEnabled) {
        result.put(RuleBasedRouteDTO.CUSTODIAN_KEY, ruleBasedRouteDTO.custodian());
      }
      return Collections.unmodifiableMap(result);
    }

    return body;
  }
}
