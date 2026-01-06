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

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.rules.model.Result;
import de.gematik.demis.nrs.rules.model.Rule;
import de.gematik.demis.nrs.rules.model.RulesConfig;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RulesService {

  private final FhirContext context;
  private final RulesConfig rulesConfig;
  private final NrsConfigProps props;

  public Optional<Result> evaluateRules(Bundle bundle) {
    IFhirPath fhirPath = context.newFhirPath();
    CustomEvaluationContext evaluationContext = new CustomEvaluationContext(bundle);
    fhirPath.setEvaluationContext(evaluationContext);

    Map<String, Rule> rules = rulesConfig.rules();
    Rule root = rules.get(props.rulesStartId());

    return evaluateRule(root, fhirPath, bundle);
  }

  /**
   * Recursively evaluates a given rule against a FHIR bundle using FHIRPath expressions.
   *
   * @param rule The rule to be evaluated.
   * @param fhirPath The FHIRPath object used to evaluate FHIRPath expressions.
   * @param bundle The FHIR bundle containing the data to be evaluated.
   * @return An Optional containing the Result if the rule evaluation leads to a result, otherwise
   *     an empty Optional.
   */
  private Optional<Result> evaluateRule(Rule rule, IFhirPath fhirPath, Bundle bundle) {
    if (rule.checkRule(fhirPath, bundle)) {
      return getResult(rule, "conditionFulfilled")
          .or(() -> evaluateSubrule(rule, "conditionFulfilled", fhirPath, bundle));
    } else {
      return getResult(rule, "conditionNotMet")
          .or(() -> evaluateSubrule(rule, "conditionNotMet", fhirPath, bundle));
    }
  }

  private Optional<Result> getResult(Rule rule, String condition) {
    return rule.resultExists() && rule.result().containsKey(condition)
        ? Optional.of(rulesConfig.results().get(rule.result().get(condition)))
        : Optional.empty();
  }

  private Optional<Result> evaluateSubrule(
      Rule rule, String condition, IFhirPath fhirPath, Bundle bundle) {
    return evaluateRule(
        rulesConfig.rules().get(rule.followingRules().get(condition)), fhirPath, bundle);
  }
}
