package de.gematik.demis.nrs.rules;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Route;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class RulesLoader {

  @Bean
  RulesConfig rulesConfig(final NrsConfigProps props) {
    try {
      String rulesPath = props.routingRules();
      String rulesContent = Files.readString(Path.of(rulesPath));
      ObjectMapper mapper = new ObjectMapper();
      RulesConfig rulesConfig = mapper.readValue(rulesContent, RulesConfig.class);
      log.info("Loaded routing rules config from {}", rulesPath);
      checkRules(rulesConfig);
      return rulesConfig;
    } catch (IOException e) {
      log.warn("Error while processing routing rules config file", e);
    }
    return new RulesConfig(new LinkedHashMap<>(), new LinkedHashMap<>());
  }

  private void checkRules(RulesConfig rulesConfig) {
    if (rulesConfig.rules().isEmpty()) {
      log.warn("No rules found in routing rules config");
    }
    if (rulesConfig.results().isEmpty()) {
      log.warn("No results found in routing rules config");
    }
    Collection<Result> values = rulesConfig.results().values();
    for (Result result : values) {
      List<Route> routes = result.routesTo();
      for (Route route : routes) {
        if (route.actions().isEmpty()) {
          log.error("No actions found in routing rules config " + result.description());
        }
      }
    }
  }
}
