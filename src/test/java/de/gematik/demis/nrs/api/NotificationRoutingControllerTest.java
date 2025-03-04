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
 * #L%
 */

import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_CURRENT;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_ORDINARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_OTHER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.SUBMITTER;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.*;
import static de.gematik.demis.nrs.rules.model.RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS;
import static java.util.Map.entry;
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

import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.service.NotificationRoutingService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import de.gematik.demis.service.base.error.rest.ErrorHandlerConfiguration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationRoutingController.class)
@Import(ErrorHandlerConfiguration.class)
class NotificationRoutingControllerTest {

  private static final String URL_DETERMINE_ROUTING = "/routing";
  private static final String URL_DETERMINE_RULE_BASED_ROUTING = "/routing/v2";
  private static final String URL_FIND_HEALTH_OFFICE_BY_ADDRESS = "/routing/health-office";

  @Autowired private MockMvc mockMvc;

  @MockBean private NotificationRoutingService notificationRoutingService;
  @MockBean private AddressToHealthOfficeLookup healthDepartmentLookupService;

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
  void determineRouting_success() throws Exception {
    final String body = "valid fhir notification as json";
    final RoutingOutput routingOutput =
        new RoutingOutput(
            Map.ofEntries(
                entry(NOTIFIER, "1.1"),
                entry(NOTIFIED_PERSON_CURRENT, "1.2"),
                entry(NOTIFIED_PERSON_ORDINARY, "1.3"),
                entry(NOTIFIED_PERSON_PRIMARY, "1.4"),
                entry(NOTIFIED_PERSON_OTHER, "1.5"),
                entry(SUBMITTER, "1.6")),
            "1.4");

    when(notificationRoutingService.determineRouting(body)).thenReturn(routingOutput);

    final String expected =
        """
            {"healthOffices":
                {
                 "NOTIFIED_PERSON_PRIMARY":"1.4",
                 "NOTIFIED_PERSON_ORDINARY":"1.3",
                 "NOTIFIED_PERSON_CURRENT":"1.2",
                 "NOTIFIED_PERSON_OTHER":"1.5",
                 "NOTIFIER":"1.1",
                 "SUBMITTER":"1.6"
                },
             "responsible":"1.4"}
            """;

    mockMvc
        .perform(post(URL_DETERMINE_ROUTING).contentType(APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(content().json(expected));
  }

  @Test
  void determineRouting_emptyRequestBody() throws Exception {
    mockMvc
        .perform(post(URL_DETERMINE_ROUTING).contentType(APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  void determineRuleBasedRouting_success() throws Exception {
    final String body = "valid fhir notification as json";
    final RuleBasedRouteDTO routingOutput =
        new RuleBasedRouteDTO(
            "laboratory",
            "7.1",
            List.of(
                new Route(RESPONSIBLE_HEALTH_OFFICE, "7.1", List.of("encrypt"), false),
                new Route(RESPONSIBLE_HEALTH_OFFICE_SORMAS, "7.1", List.of("encrypt"), true)),
            null,
            null);

    when(notificationRoutingService.determineRuleBasedRouting(eq(body), anyBoolean(), anyString()))
        .thenReturn(routingOutput);

    final String expected =
        """
            {
              "type": "laboratory",
              "notificationCategory": "7.1",
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
              "responsible": null
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
}
