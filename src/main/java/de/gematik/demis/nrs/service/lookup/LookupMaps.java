package de.gematik.demis.nrs.service.lookup;

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

import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class LookupMaps {

  @RequiredArgsConstructor
  @Getter
  enum LookupMap {
    POSTALCODE("PLZ_2_GA.csv"),
    POSTALCODE_CITY("PLZ_CITY_2_GA.csv"),
    POSTALCODE_CITY_STREET("PLZ_CITY_STREET_2_GA.csv"),
    POSTALCODE_CITY_STREET_NO("PLZ_CITY_STREET_NR_2_GA.csv"),

    CITY("CITY_2_GA.csv"),
    CITY_STREET("CITY_STREET_2_GA.csv"),
    CITY_STREET_NO("CITY_STREET_NR_2_GA.csv"),

    FALLBACK_POSTALCODE("PLZ_Adresse_GA_MANY_ROWCOUNT.csv"),
    FALLBACK_POSTALCODE_CITY("PLZ_CITY_Adresse_GA_MANY_ROWCOUNT.csv"),
    FALLBACK_POSTALCODE_CITY_STREET("PLZ_CITY_STREET_Adresse_GA_MANY_ROWCOUNT.csv");

    private final String csvFilename;
  }

  private static final String KEY_PART_DELIMITER = "_";

  private final Map<LookupMap, Map<String, String>> maps;

  public String getValue(final LookupMap map, final String[] keyParts) {
    return maps.get(map).get(String.join(KEY_PART_DELIMITER, keyParts));
  }
}
