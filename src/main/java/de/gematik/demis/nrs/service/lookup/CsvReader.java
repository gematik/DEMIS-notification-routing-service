package de.gematik.demis.nrs.service.lookup;

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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
class CsvReader {

  public static final String CSV_SEPARATOR = ",";
  private static final Charset CSV_CHARSET =
      StandardCharsets.ISO_8859_1; // TODO sollte eigentlich UTF_8 sein!!!!

  public Map<String, String> readKeyValueFile(final Path path) throws IOException {
    try (final Stream<String> lines = Files.lines(path, CSV_CHARSET)) {
      return lines
          .map(line -> line.split(CSV_SEPARATOR, 2))
          .filter(cols -> cols.length == 2)
          // TODO duplicate keys ignorieren???
          .collect(Collectors.toUnmodifiableMap(cols -> cols[0], cols -> cols[1]));
    }
  }
}
