package de.gematik.demis.nrs.service;

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

/** definitions of some exceptions * */
public final class ExceptionMessages {
  public static final String NO_RESULT_FOR_RULE_EVALUATION =
      "NRS-001: no results found for bundle identifier '%s'.";
  public static final String NULL_FOR_SPECIFIC_RECEIVER_ID_IS_NOT_ALLOWED_FOR_TYPE =
      "NRS-002: null for specific receiver id is not allowed for type '%s'.";
  public static final String NO_HEALTH_OFFICE_FOUND =
      "NRS-003: no responsible health department found.";
  public static final String LOOKUP_FOR_RULE_RESULT_TYPE_IS_NOT_SUPPORTED =
      "NRS-004: handling of rule result type '%s' is not supported.";
  public static final String NO_OPTIONAL_HEALTH_OFFICE_FOUND =
      "NRS-005:No health office found for optional route with type '{}'";
}
