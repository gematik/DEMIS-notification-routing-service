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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RulesResultTypeEnum {
  @JsonProperty("responsible_health_office")
  RESPONSIBLE_HEALTH_OFFICE("responsible_health_office", 1),
  @JsonProperty("responsible_health_office_sormas")
  RESPONSIBLE_HEALTH_OFFICE_SORMAS("responsible_health_office_sormas", 4),
  @JsonProperty("responsible_health_office_with_relates_to")
  RESPONSIBLE_HEALTH_OFFICE_WITH_RELATES_TO("responsible_health_office_with_relates_to", 3),
  @JsonProperty("specific_receiver")
  SPECIFIC_RECEIVER("specific_receiver", 5),
  OTHER("", 99),
  @JsonProperty("responsible_health_office_tuberculosis")
  RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS("responsible_health_office_tuberculosis", 2);

  private static final Map<String, RulesResultTypeEnum> CODE_TO_ENUM =
      Arrays.stream(values())
          .collect(Collectors.toMap(RulesResultTypeEnum::getCode, Function.identity()));

  private final String code;

  private final int priority;
}
