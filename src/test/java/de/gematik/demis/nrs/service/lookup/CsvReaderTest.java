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

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import de.gematik.demis.nrs.test.FileUtil;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvReaderTest {

  private static final String CSV_FILE = "/lookup/csv-reader-test.csv";

  private final CsvReader underTest = new CsvReader();

  @Test
  void read() {
    final Path path = FileUtil.resourceAsPath(CSV_FILE);
    final Map<String, String> result = assertDoesNotThrow(() -> underTest.readKeyValueFile(path));
    assertThat(result)
        .containsOnly(
            entry("10119_BERLIN_TESTSTR._1", "1.11.0.03.01."),
            entry("10119_BERLIN_TESTSTR._2", "1.11.0.03.02."));
  }
}
