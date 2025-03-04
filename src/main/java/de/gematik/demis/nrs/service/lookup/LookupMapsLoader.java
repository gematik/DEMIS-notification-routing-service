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

import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
class LookupMapsLoader {

  @Bean
  LookupMaps lookupMaps(final CsvReader csvReader, final NrsConfigProps props) throws IOException {
    final Path dataDirectory = Path.of(props.lookupDataDirectory());
    final Map<LookupMap, Map<String, String>> maps = new EnumMap<>(LookupMap.class);
    for (final LookupMap lookupMapType : LookupMap.values()) {
      final Path path = dataDirectory.resolve(lookupMapType.getCsvFilename());
      final Map<String, String> map;
      try {
        map = csvReader.readKeyValueFile(path);
      } catch (final Exception e) {
        log.error("error loading lookup data {} from {}", lookupMapType, path, e);
        throw e;
      }
      log.info("Loading {} from {} -> {} entries", lookupMapType.name(), path, map.size());
      maps.put(lookupMapType, map);
    }
    return new LookupMaps(maps);
  }
}
