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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class RulesLoader {

  private final boolean notifications73;
  private final boolean followUpNotifications;
  private final ObjectMapper objectMapper;

  public RulesLoader(
      @Value("${feature.flag.notifications.7.3:false}") boolean notifications73,
      @Value("${feature.flag.follow.up.notification:false}") boolean followUpNotifications,
      final ObjectMapper objectMapper) {
    this.notifications73 = notifications73;
    this.followUpNotifications = followUpNotifications;
    this.objectMapper = objectMapper;
  }

  @Bean
  RulesConfig rulesConfig(final NrsConfigProps props) {
    String rulesPath = props.routingRules();
    if (followUpNotifications) {
      rulesPath = props.routingRulesWithFollowUp();
    } else if (notifications73) {
      rulesPath = props.routingRules73enabled();
    }
    log.info("Loading routing rules config from '{}'", rulesPath);

    try {
      String rulesContent = Files.readString(Path.of(rulesPath));
      RulesConfig rulesConfig = objectMapper.readValue(rulesContent, RulesConfig.class);
      checkRules(rulesConfig);
      log.info("Successfully routing rules config from '{}'", rulesPath);
      return rulesConfig;
    } catch (IOException | IllegalStateException e) {
      final String s =
          String.format("Error while processing routing rules config file '%s'", rulesPath);
      throw new IllegalStateException(s, e);
    }
  }

  private void checkRules(RulesConfig rulesConfig) {
    boolean configHasErrors = false;
    if (rulesConfig.rules().isEmpty()) {
      log.error("No rules found in routing rules config");
      configHasErrors = true;
    }
    if (rulesConfig.results().isEmpty()) {
      log.error("No results found in routing rules config");
      configHasErrors = true;
    }
    Collection<Result> values = rulesConfig.results().values();
    for (Result result : values) {
      List<Route> routes = result.routesTo();
      for (Route route : routes) {
        if (route.actions().isEmpty()) {
          log.error("No actions found in routing rules config " + result.description());
          configHasErrors = true;
        }
      }
    }

    if (configHasErrors) {
      throw new IllegalStateException();
    }
  }
}
