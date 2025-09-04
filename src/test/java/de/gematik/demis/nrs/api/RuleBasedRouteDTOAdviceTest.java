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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.util.SequencedSets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

class RuleBasedRouteDTOAdviceTest {

  private static Object getResult(final RuleBasedRouteDTOAdvice advice, final Object body) {
    return advice.beforeBodyWrite(
        body,
        mock(MethodParameter.class),
        mock(MediaType.class),
        MappingJackson2HttpMessageConverter.class,
        mock(ServerHttpRequest.class),
        mock(ServerHttpResponse.class));
  }

  private static RuleBasedRouteDTO createTestDTO() {
    return new RuleBasedRouteDTO(
        "test-type",
        "test-category",
        SequencedSets.of(),
        List.of(),
        Map.of(),
        "test-responsible",
        Set.of("ROLE_TEST"),
        "test-custodian");
  }

  @Test
  void thatRuleBasedRouteDTOIsSupported() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(true, true);
    final MethodParameter methodParameter = mock(MethodParameter.class);
    doReturn(RuleBasedRouteDTO.class).when(methodParameter).getParameterType();
    boolean supports = advice.supports(methodParameter, MappingJackson2HttpMessageConverter.class);

    assertThat(supports).isTrue();
  }

  @Test
  void thatOtherTypesAreNotSupported() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(true, true);
    final MethodParameter methodParameter = mock(MethodParameter.class);
    doReturn(String.class).when(methodParameter).getParameterType();

    final boolean supports =
        advice.supports(methodParameter, MappingJackson2HttpMessageConverter.class);

    assertThat(supports).isFalse();
  }

  @Test
  void thatAllOptionalFieldsAreIncludedWhenFeatureFlagsAreEnabled() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(true, true);
    final RuleBasedRouteDTO dto = createTestDTO();

    final Object result = getResult(advice, dto);

    assertThat(result)
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
        .satisfies(
            map -> {
              assertThat(map).containsKey(RuleBasedRouteDTO.ALLOWED_ROLES_KEY);
              assertThat(map).containsKey(RuleBasedRouteDTO.CUSTODIAN_KEY);
            });
  }

  @Test
  void thatAllowedRolesIsExcludedWhenPermissionFlagDisabled() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(false, true);
    final RuleBasedRouteDTO dto = createTestDTO();

    final Object result = getResult(advice, dto);

    assertThat(result)
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
        .doesNotContainKey(RuleBasedRouteDTO.ALLOWED_ROLES_KEY);
  }

  @Test
  void thatCustodianIsExcludedWhenCustodianFlagDisabled() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(true, false);
    final RuleBasedRouteDTO dto = createTestDTO();

    final Object result = getResult(advice, dto);

    assertThat(result)
        .asInstanceOf(InstanceOfAssertFactories.map(String.class, Object.class))
        .doesNotContainKey(RuleBasedRouteDTO.CUSTODIAN_KEY);
  }

  @Test
  void shouldReturnBodyUnchangedForNonRuleBasedRouteDTO() {
    final RuleBasedRouteDTOAdvice advice = new RuleBasedRouteDTOAdvice(true, true);
    final String nonDtoBody = "some string";

    final Object result = getResult(advice, nonDtoBody);

    assertThat(result).isEqualTo(nonDtoBody);
  }
}
