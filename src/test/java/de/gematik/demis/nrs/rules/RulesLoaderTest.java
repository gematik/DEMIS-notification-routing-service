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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.rules.model.ActionType;
import de.gematik.demis.nrs.rules.model.Rule;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

class RulesLoaderTest {
  final List<String> allResults =
      List.of(
          "laboratory_7_1",
          "laboratory_7_1_covid",
          "laboratory_7_1_tuberculosis",
          "laboratory_7_1_anonymous_follow_up",
          "laboratory_7_3",
          "laboratory_7_3_followup",
          "laboratory_7_3_anonymous",
          "disease_6_1",
          "disease_6_1_covid",
          "disease_6_1_tuberculosis",
          "disease_6_1_anonymous_follow_up",
          "disease_7_3",
          "disease_7_3_followup");

  final List<String> results73 =
      List.of(
          "laboratory_7_3",
          "laboratory_7_3_followup",
          "laboratory_7_3_anonymous",
          "disease_7_3",
          "disease_7_3_followup");

  @Test
  void loadsDefaultConfigIfNoFlags() {
    NrsConfigProps props = mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");

    RulesLoader loader = new RulesLoader(false, false, new ObjectMapper());
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.toString()).contains("laboratory_7_1");

    final List<String> covidResults = List.of("laboratory_7_1_covid", "disease_6_1_covid");
    covidResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route
                                    .type()
                                    .equals(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS)))
                .isTrue());

    allResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.PSEUDO_COPY)
                                    && !route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());
  }

  @Test
  void loadsExcerptEncryptionConfig() {
    NrsConfigProps props = mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");
    Mockito.when(props.routingRulesExcerptEncryptionEnabled())
        .thenReturn("rules/routingConfig_excerptEncryption.json");

    RulesLoader loader = new RulesLoader(true, false, new ObjectMapper());
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.rules())
        .hasSize(14)
        .as("rules contain specific keys for §7.3 processing")
        .containsKeys(
            "laboratory_distinguish_7_3_from_7_4",
            "disease_distinguish_7_3_followup_from_7_3_regular");
    assertThat(config.results())
        .hasSize(14)
        .as("results contain specific keys for §7.3 processing")
        .containsKeys("laboratory_7_3", "disease_7_3");

    assertThat(config.results().get("disease_6_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);
    assertThat(config.results().get("laboratory_7_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);

    final List<String> covidResults = List.of("laboratory_7_1_covid", "disease_6_1_covid");
    covidResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route
                                    .type()
                                    .equals(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS)))
                .isTrue());

    allResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.PSEUDO_COPY)
                                    && route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());

    results73.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.REPRODUCE)
                                    && route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());
  }

  @Test
  void loadsRemoveSormasExcerptsConfig() {
    NrsConfigProps props = mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");
    Mockito.when(props.routingRulesRemoveSormasExcerpts())
        .thenReturn("rules/routingConfig_removeSormasExcerpts.json");

    RulesLoader loader = new RulesLoader(false, true, new ObjectMapper());
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.rules())
        .hasSize(14)
        .as("rules contain specific keys for §7.3 processing")
        .containsKeys(
            "laboratory_distinguish_7_3_from_7_4",
            "disease_distinguish_7_3_followup_from_7_3_regular");
    assertThat(config.results())
        .hasSize(14)
        .as("results contain specific keys for §7.3 processing")
        .containsKeys("laboratory_7_3", "disease_7_3");

    assertThat(config.results().get("disease_6_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);
    assertThat(config.results().get("laboratory_7_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);

    final List<String> covidResults = List.of("laboratory_7_1_covid", "disease_6_1_covid");
    covidResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .noneMatch(
                            route ->
                                route
                                    .type()
                                    .equals(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS)))
                .isTrue());

    allResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.PSEUDO_COPY)
                                    && !route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());

    results73.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.REPRODUCE)
                                    && route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());
  }

  @Test
  void loadsExcerptEncryptionEnabledRemoveSormasExcerptsConfig() {
    NrsConfigProps props = mock(NrsConfigProps.class);
    Mockito.when(props.routingRules()).thenReturn("rules/routingConfig.json");
    Mockito.when(props.routingRulesExcerptEncryptionEnabledRemoveSormasExcerpts())
        .thenReturn("rules/routingConfig_excerptEncryption_removeSormasExcerpts.json");

    RulesLoader loader = new RulesLoader(true, true, new ObjectMapper());
    RulesConfig config = loader.rulesConfig(props);

    assertThat(config).isNotNull();
    assertThat(config.rules())
        .hasSize(14)
        .as("rules contain specific keys for §7.3 processing")
        .containsKeys(
            "laboratory_distinguish_7_3_from_7_4",
            "disease_distinguish_7_3_followup_from_7_3_regular");
    assertThat(config.results())
        .hasSize(14)
        .as("results contain specific keys for §7.3 processing")
        .containsKeys("laboratory_7_3", "disease_7_3");

    assertThat(config.results().get("disease_6_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);
    assertThat(config.results().get("laboratory_7_1_anonymous_follow_up"))
        .extracting("routesTo", InstanceOfAssertFactories.LIST)
        .hasSize(2);

    final List<String> covidResults = List.of("laboratory_7_1_covid", "disease_6_1_covid");
    covidResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .noneMatch(
                            route ->
                                route
                                    .type()
                                    .equals(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS)))
                .isTrue());

    allResults.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.PSEUDO_COPY)
                                    && route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());
    results73.forEach(
        result ->
            assertThat(
                    config.results().get(result).routesTo().stream()
                        .anyMatch(
                            route ->
                                route.type().equals(RulesResultTypeEnum.SPECIFIC_RECEIVER)
                                    && route.actions().contains(ActionType.REPRODUCE)
                                    && route.actions().contains(ActionType.ENCRYPT)))
                .isTrue());
  }

  @Test
  void thatServiceCrashesWhenConfigNotFound() {
    final RulesLoader rulesLoader = new RulesLoader(false, false, new ObjectMapper());
    assertThatIllegalStateException()
        .isThrownBy(
            () ->
                rulesLoader.rulesConfig(
                    new NrsConfigProps(
                        "/",
                        "/doesnt-exist.json",
                        "./doesnt-exist.json",
                        "./doesnt-exist.json",
                        "./doesnt-exist.json",
                        "-")));
  }

  @MethodSource("invalidRules")
  @ParameterizedTest
  void thatServiceCrashesWhenNoRulesAndResults(final RulesConfig input)
      throws JsonProcessingException {
    final ObjectMapper objectMapper = mock(ObjectMapper.class);
    when(objectMapper.readValue(anyString(), eq(RulesConfig.class))).thenReturn(input);

    final RulesLoader rulesLoader = new RulesLoader(false, false, objectMapper);
    final NrsConfigProps props =
        new NrsConfigProps(
            "/",
            "rules/routingConfig.json",
            "./doesnt-exist.json",
            "./doesnt-exist.json",
            "./doesnt-exist.json",
            "-");
    assertThatThrownBy(() -> rulesLoader.rulesConfig(props))
        .isInstanceOf(IllegalStateException.class);
    verify(objectMapper, Mockito.times(1)).readValue(anyString(), eq(RulesConfig.class));
  }

  private static Stream<RulesConfig> invalidRules() {
    return Stream.of(
        new RulesConfig(Map.of(), Map.of()),
        new RulesConfig(Map.of("start", new Rule("", "", "", Map.of(), Map.of())), Map.of()));
  }
}
