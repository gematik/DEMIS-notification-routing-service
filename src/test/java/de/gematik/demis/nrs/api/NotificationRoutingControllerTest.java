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

import static de.gematik.demis.nrs.api.dto.BundleActionType.CREATE_PSEUDONYM_RECORD;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.SPECIFIC_RECEIVER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.demis.nrs.api.dto.BundleAction;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.model.ActionType;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.service.NotificationRoutingLegacyService;
import de.gematik.demis.nrs.service.NotificationRoutingService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.nrs.util.SequencedSets;
import de.gematik.demis.service.base.error.rest.ErrorHandlerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {NotificationRoutingController.class},
    properties = {
      "feature.flag.tuberculosis.routing.enabled=true",
      "feature.flag.permission.check.enabled=true"
    })
@Import(ErrorHandlerConfiguration.class)
class NotificationRoutingControllerTest {

  private static final String URL_DETERMINE_RULE_BASED_ROUTING = "/routing/v2";
  private static final String URL_FIND_HEALTH_OFFICE_BY_ADDRESS = "/routing/health-office";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private NotificationRoutingService notificationRoutingService;
  @MockitoBean private NotificationRoutingLegacyService notificationRoutingLegacyService;
  @MockitoBean private AddressToHealthOfficeLookup healthDepartmentLookupService;

  @Test
  void findHealthOfficeByAddress_success() throws Exception {
    final AddressDTO address = new AddressDTO("TestStr.", "1a", "12345", "berlin", "DEU");
    final String healthOffice = "1.23.45";
    when(healthDepartmentLookupService.lookup(address)).thenReturn(Optional.of(healthOffice));
    mockMvc
        .perform(
            get(URL_FIND_HEALTH_OFFICE_BY_ADDRESS)
                .queryParam("countryCode", address.countryCode())
                .queryParam("postalCode", address.postalCode())
                .queryParam("city", address.city())
                .queryParam("street", address.street())
                .queryParam("no", address.no()))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.equalTo(healthOffice)));
  }

  @Test
  void findHealthOfficeByAddress_notFound() throws Exception {
    when(healthDepartmentLookupService.lookup(any())).thenReturn(Optional.empty());
    mockMvc
        .perform(get(URL_FIND_HEALTH_OFFICE_BY_ADDRESS))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.blankString()));
  }

  @Test
  void determineRuleBasedRouting_success() throws Exception {
    final String body = "valid fhir notification as json";
    final RuleBasedRouteDTO routingOutput =
        new RuleBasedRouteDTO(
            "laboratory",
            "7.1",
            SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
            List.of(
                new Route(RESPONSIBLE_HEALTH_OFFICE, "7.1", List.of(ActionType.ENCRYPT), false),
                new Route(
                    RESPONSIBLE_HEALTH_OFFICE_SORMAS, "7.1", List.of(ActionType.ENCRYPT), true)),
            null,
            null,
            Set.of("role_1", "role_2"),
            null);

    when(notificationRoutingService.determineRuleBasedRouting(eq(body), anyBoolean(), anyString()))
        .thenReturn(routingOutput);

    final String expected =
        """
            {
              "type": "laboratory",
              "notificationCategory": "7.1",
              "bundleActions": [{"type": "create_pseudonym_record", "optional": true}],
              "routes": [
                {
                  "type": "responsible_health_office",
                  "specificReceiverId": "7.1",
                  "actions": [
                    "encrypt"
                  ],
                  "optional": false
                },
                {
                  "type": "responsible_health_office_sormas",
                  "specificReceiverId": "7.1",
                  "actions": [
                    "encrypt"
                  ],
                  "optional": true
                }
              ],
              "healthOffices": null,
              "responsible": null,
              "allowedRoles": ["role_1", "role_2"]
            }
            """;

    mockMvc
        .perform(
            post(URL_DETERMINE_RULE_BASED_ROUTING)
                .contentType(APPLICATION_JSON)
                .content(body)
                .param("isTestUser", "false")
                .param("testUserID", ""))
        .andExpect(status().isOk())
        .andExpect(content().json(expected));
  }

  @Test
  void determineRuleBasedRouting_emptyRequestBody() throws Exception {
    mockMvc
        .perform(post(URL_DETERMINE_RULE_BASED_ROUTING).contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Nested
  class PermissionRelated {
    @Test
    void thatNoPermissionsAreReturnedWhenFeatureFlagDisabled() {
      final RuleBasedRouteDTO routingOutput =
          new RuleBasedRouteDTO(
              "laboratory",
              "7.1",
              SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
              List.of(new Route(SPECIFIC_RECEIVER, "1.", List.of(ActionType.ENCRYPT), false)),
              Map.of(),
              "1.",
              Set.of("role_1", "role_2"),
              null);

      when(notificationRoutingService.determineRuleBasedRouting("any", false, "any"))
          .thenReturn(routingOutput);

      final NotificationRoutingController controller =
          new NotificationRoutingController(
              notificationRoutingService,
              notificationRoutingLegacyService,
              healthDepartmentLookupService,
              true);
      final Object actual = controller.determineRuleBasedRouting("any", false, "any");
      assertThat(actual).isInstanceOf(RuleBasedRouteDTO.class);
    }

    @Test
    void thatPermissionsAreReturnedWhenFeatureFlagEnabled() {
      final RuleBasedRouteDTO routingOutput =
          new RuleBasedRouteDTO(
              "laboratory",
              "7.1",
              SequencedSets.of(BundleAction.optionalOf(CREATE_PSEUDONYM_RECORD)),
              List.of(new Route(SPECIFIC_RECEIVER, "1.", List.of(ActionType.ENCRYPT), false)),
              Map.of(),
              "1.",
              Set.of("a permission", "another permission"),
              null);

      when(notificationRoutingService.determineRuleBasedRouting("any", false, "any"))
          .thenReturn(routingOutput);

      final NotificationRoutingController controller =
          new NotificationRoutingController(
              notificationRoutingService,
              notificationRoutingLegacyService,
              healthDepartmentLookupService,
              true);
      final Object actual = controller.determineRuleBasedRouting("any", false, "any");
      assertThat(actual).isInstanceOf(RuleBasedRouteDTO.class);
      assertThat(actual)
          .isInstanceOfSatisfying(
              RuleBasedRouteDTO.class,
              (actualResult) -> {
                assertThat(actualResult.allowedRoles())
                    .containsExactlyInAnyOrder("a permission", "another permission");
              });
    }
  }
}
