package de.gematik.demis.nrs.rules;

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

import de.gematik.demis.nrs.rules.model.Rule;
import java.util.HashMap;
import java.util.Map;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

/** A deserializer that will set the key of the map as id on a {@link Rule} */
public class RuleMapDeserializer extends ValueDeserializer<Map<String, Rule>> {
  @Override
  public Map<String, Rule> deserialize(JsonParser p, DeserializationContext ctxt)
      throws JacksonException {
    Map<String, Rule> ruleMap = new HashMap<>();

    if (p.currentToken() != JsonToken.START_OBJECT) {
      p.nextToken();
    }

    while (p.nextToken() != JsonToken.END_OBJECT) {
      String ruleId = p.currentName();
      p.nextToken(); // Move to the rule value

      Rule rule = p.readValueAs(Rule.class);
      rule = Rule.replaceRuleId(rule, ruleId); // Set the key as a property

      ruleMap.put(ruleId, rule);
    }

    return ruleMap;
  }
}
