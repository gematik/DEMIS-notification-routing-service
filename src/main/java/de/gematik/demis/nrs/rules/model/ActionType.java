package de.gematik.demis.nrs.rules.model;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ActionType {
  ENCRYPT("encrypt"),
  STORE_DESTINATION("store_destination"),
  NO_ACTION("no_action"),
  PSEUDO_COPY("pseudo_copy"),
  REPRODUCE("reproduce"),
  CREATE_PSEUDONYM_RECORD("create_pseudonym_record"),
  PSEUDO_ORIGINAL("pseudo_original");

  private final String value;

  ActionType(String value) {
    this.value = value;
  }

  @JsonCreator
  public static ActionType fromValue(String value) {
    for (ActionType action : ActionType.values()) {
      if (action.value.equalsIgnoreCase(value)) {
        return action;
      }
    }
    throw new IllegalArgumentException("Unknown action type: " + value);
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
