package de.gematik.demis.nrs.integrationtest;

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

import static de.gematik.demis.nrs.test.FileUtil.readResource;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.gematik.demis.nrs.NotificationRoutingApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.UseMainMethod;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    classes = NotificationRoutingApplication.class,
    useMainMethod = UseMainMethod.ALWAYS,
    webEnvironment = WebEnvironment.MOCK,
    properties = "nrs.lookup-data-directory=src/test/resources/integrationtest/data/lookup")
@AutoConfigureMockMvc
class NrsIntegrationTest {
  private static final String LABORATORY_NOTIFICATION =
      "/integrationtest/requests/laboratory-notification.json";
  private static final String LABORATORY_NOTIFICATION_BLANK =
      "/integrationtest/requests/laboratory-notification.json";
  private static final String DISEASE_NOTIFICATION =
      "/integrationtest/requests/disease-notification.json";
  private static final String NOTIFICATION_NOT_PROCESSABLE =
      "/integrationtest/requests/not-processable.json";

  @Autowired MockMvc mockMvc;

  @Test
  void routing_success_laboratory() throws Exception {
    mockMvc
        .perform(
            post("/routing")
                .contentType(APPLICATION_JSON)
                .content(readResource(LABORATORY_NOTIFICATION)))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                {"healthOffices": {
                     "NOTIFIED_PERSON_PRIMARY":"1.10",
                     "NOTIFIED_PERSON_ORDINARY":"3.14",
                     "NOTIFIED_PERSON_CURRENT":"1.13",
                     "NOTIFIER":"3.14",
                     "SUBMITTER":"3.14"
                  },
                  "responsible":"1.13"
                 }
"""));
  }

  @Test
  void routing_success_disease() throws Exception {
    mockMvc
        .perform(
            post("/routing")
                .contentType(APPLICATION_JSON)
                .content(readResource(DISEASE_NOTIFICATION)))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                {"healthOffices": {
                     "NOTIFIED_PERSON_PRIMARY":"1.17",
                     "NOTIFIED_PERSON_CURRENT":"5.6.7",
                     "NOTIFIER":"5.6.7"
                  },
                  "responsible":"5.6.7"
                 }
"""));
  }

  @Test
  void invalidNotification() throws Exception {
    mockMvc
        .perform(post("/routing").contentType(APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void routing_returns_200_with_empty_health_office_list() throws Exception {
    mockMvc
        .perform(
            post("/routing/v2")
                .contentType(APPLICATION_JSON)
                .content(readResource(NOTIFICATION_NOT_PROCESSABLE))
                .param("isTestUser", "false")
                .param("testUserID", ""))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                   {
                     "type": "laboratory",
                     "notificationCategory": "7.1",
                     "routes": [
                       {
                         "type": "specific_receiver",
                         "specificReceiverId": "1.",
                         "actions": [
                           "pseudo_copy"
                         ],
                         "optional": false
                       }
                     ],
                     "healthOffices": null,
                     "responsible": null
                   }"""));
  }

  @Test
  void routing_special_case_blank() throws Exception {
    mockMvc
        .perform(
            post("/routing/v2")
                .contentType(APPLICATION_JSON)
                .content(readResource(LABORATORY_NOTIFICATION_BLANK))
                .param("isTestUser", "false")
                .param("testUserID", ""))
        .andExpect(status().isOk());
  }

  @Test
  void routing_success_laboratory_rulesBased() throws Exception {
    mockMvc
        .perform(
            post("/routing/v2")
                .contentType(APPLICATION_JSON)
                .content(readResource(LABORATORY_NOTIFICATION))
                .param("isTestUser", "false")
                .param("testUserID", ""))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                                {
                                    "type": "laboratory",
                                    "notificationCategory": "7.1",
                                    "routes": [
                                      {
                                        "type": "specific_receiver",
                                        "specificReceiverId": "1.",
                                        "actions": [
                                          "pseudo_copy"
                                        ],
                                        "optional": false
                                      },
                                      {
                                        "type": "responsible_health_office",
                                        "specificReceiverId": "1.13",
                                        "actions": [
                                          "encryption"
                                        ],
                                        "optional": false
                                      },
                                      {
                                        "type": "responsible_health_office_sormas",
                                        "specificReceiverId": "2.13",
                                        "actions": [
                                          "encryption"
                                        ],
                                        "optional": true
                                      }
                                    ],
                                    "healthOffices": {
                                      "NOTIFIED_PERSON_CURRENT": "1.13",
                                      "NOTIFIED_PERSON_ORDINARY": "3.14",
                                      "NOTIFIED_PERSON_PRIMARY": "1.10",
                                      "SUBMITTER": "3.14",
                                      "NOTIFIER": "3.14"
                                    },
                                    "responsible": "1.13"
                                  }
                """));
  }

  @Test
  void routing_success_disease_rules_based() throws Exception {
    mockMvc
        .perform(
            post("/routing/v2")
                .contentType(APPLICATION_JSON)
                .content(readResource(DISEASE_NOTIFICATION))
                .param("isTestUser", "false")
                .param("testUserID", ""))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    """
                                {
                                  "type": "disease",
                                  "notificationCategory": "6.1",
                                  "routes": [
                                    {
                                      "type": "specific_receiver",
                                      "specificReceiverId": "1.",
                                      "actions": [
                                        "pseudo_copy"
                                      ],
                                      "optional": false
                                    },
                                    {
                                      "type": "responsible_health_office",
                                      "specificReceiverId": "5.6.7",
                                      "actions": [
                                        "encryption"
                                      ],
                                      "optional": false
                                    },
                                    {
                                      "type": "responsible_health_office_sormas",
                                      "specificReceiverId": "2.6.7",
                                      "actions": [
                                        "encryption"
                                      ],
                                      "optional": true
                                    }
                                  ],
                                  "healthOffices": {
                                     "NOTIFIED_PERSON_PRIMARY":"1.17",
                                     "NOTIFIED_PERSON_CURRENT":"5.6.7",
                                     "NOTIFIER":"5.6.7"
                                  },
                                  "responsible":"5.6.7"
                                }
                """));
  }
}
