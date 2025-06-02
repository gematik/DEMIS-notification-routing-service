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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.nrs.config.NrsConfigProps;
import de.gematik.demis.nrs.service.dto.LookupAddress;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import de.gematik.demis.service.base.apidoc.EnableDefaultApiSpecConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@EnableDefaultApiSpecConfig
class LookupMapsLoader {

  public final CsvReader csvReader;
  public final NrsConfigProps props;
  private final LookupMaps lookupMaps;
  private final LookupTree lookupTree;

  LookupMapsLoader(final CsvReader csvReader, final NrsConfigProps props) throws IOException {
    this.csvReader = csvReader;
    this.props = props;

    final LookupTree.Builder builder = LookupTree.builder();
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
      updateTree(lookupMapType, map, builder);
    }

    this.lookupMaps = new LookupMaps(maps);
    this.lookupTree = builder.build();
  }

  @Bean
  LookupMaps lookupMaps() {
    return lookupMaps;
  }

  @Bean
  LookupTree lookupTree() {
    return lookupTree;
  }

  private void updateTree(
      @Nonnull final LookupMap lookupMapType,
      @Nonnull final Map<String, String> map,
      @Nonnull final LookupTree.Builder builder) {
    switch (lookupMapType) {
      case POSTALCODE -> {
        final Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
          final String healthOfficeId = entry.getValue();
          if (!healthOfficeId.equals("many")) {
            final String postalCode = entry.getKey();
            builder.add(new LookupAddress(healthOfficeId, postalCode));
          }
        }
      }
      case FALLBACK_POSTALCODE,
          FALLBACK_POSTALCODE_CITY,
          FALLBACK_POSTALCODE_CITY_STREET,
          POSTALCODE_CITY,
          POSTALCODE_CITY_STREET,
          POSTALCODE_CITY_STREET_NO -> {
        final Set<Map.Entry<String, String>> entries = map.entrySet();
        for (Map.Entry<String, String> entry : entries) {
          final String healthOfficeId = entry.getValue();
          if (!healthOfficeId.equals("many")) {
            final String value = entry.getKey();
            final String[] split = value.split("_");
            builder.add(new LookupAddress(healthOfficeId, split));
          }
        }
      }
      default -> log.info("Unsupported lookup map type '{}'", lookupMapType);
    }
  }
}
