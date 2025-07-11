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

import static org.assertj.core.api.Assertions.assertThat;

import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RulesLoaderTest {

  @Test
  void loadsDefaultConfigIfNoFlags() throws Exception {
    NrsConfigProps props = Mockito.mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");

    RulesLoader loader = new RulesLoader(false, false);
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.toString().contains("notification7_1"))
        .as("Standard config should be loaded")
        .isTrue();
  }

  @Test
  void loads73ConfigIfFlagSet() throws Exception {
    NrsConfigProps props = Mockito.mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");
    Mockito.when(props.routingRules73enabled()).thenReturn("rules/routingConfig_73enabled.json");

    RulesLoader loader = new RulesLoader(true, false);
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.rules())
        .hasSize(11)
        .as("rules contain specific keys for §7.3 processing")
        .containsKeys(
            "disease_distinguish_7_3_anonymous_from_7_3_regular",
            "laboratory_distinguish_7_3_from_7_4");
    assertThat(config.results())
        .hasSize(11)
        .as("results contain specific keys for §7.3 processing")
        .containsKeys("laboratory_7_3", "disease_7_3");
  }

  @Test
  void loadsFollowUpConfigIfFlagSet() throws Exception {
    NrsConfigProps props = Mockito.mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");
    Mockito.when(props.routingRulesWithFollowUp())
        .thenReturn("rules/routingConfig_with_follow_up.json");

    RulesLoader loader = new RulesLoader(false, true);
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.rules()).hasSize(11);
    assertThat(config.results()).hasSize(11);
    assertThat(config.results().get("disease_6_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);
    assertThat(config.results().get("laboratory_7_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);
  }
}
