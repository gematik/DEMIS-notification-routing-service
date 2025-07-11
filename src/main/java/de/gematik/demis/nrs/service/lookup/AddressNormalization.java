/*
 * This code was developed in the cooperation project "Vernetzte Sicherheit" between Fraunhofer FOKUS and the German Federal Ministry of Interior
 * under the project code ÖS I 3 - 43002/1#2
 * Copyright 2014-2020 Fraunhofer FOKUS
 * Kaiserin-Augusta-Allee 31, D-10589 Berlin, Germany
 * E-Mail: espri-development@fokus.fraunhofer.de
 * All rights reserved.
 *
 * This source code is licensed under Creative Commons Attribution-NonCommercial 4.0 International license.
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 * http://creativecommons.org/licenses/by-nc/4.0/
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
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

import static de.gematik.demis.nrs.service.lookup.CsvReader.CSV_SEPARATOR;

import com.google.common.base.Strings;
import de.gematik.demis.nrs.util.sis2.SIS2TransformationService;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AddressNormalization {

  private static final char CSV_SEPARATOR_ESCAPE_CHAR = '-';

  private final SIS2TransformationService sis2TransformationService;
  private final TransliterationService transliterationService;
  private final boolean isNewSearchEnabled;

  public AddressNormalization(
      final SIS2TransformationService sis2TransformationService,
      final TransliterationService transliterationService,
      @Value("${feature.flag.search.fuzzy}") final boolean isNewSearchEnabled) {
    this.sis2TransformationService = sis2TransformationService;
    this.transliterationService = transliterationService;
    this.isNewSearchEnabled = isNewSearchEnabled;
  }

  private static String escapeCSVSeparator(String name) {
    return name.replace(CSV_SEPARATOR.charAt(0), CSV_SEPARATOR_ESCAPE_CHAR);
  }

  private static String deleteSpace(String name) {
    return name.replace(" ", "");
  }

  private static String expandStr(String streetName) {
    return streetName.replace("STR.", "STRASSE");
  }

  private static String removeNull(String name) {
    return name.equals("NULL") ? "" : name;
  }

  private String normalizeNameNoSpace(String name) {
    return normalizeName(deleteSpace(name));
  }

  private String normalizeName(String cityName) {
    if (isNewSearchEnabled) {
      // cityName might be null, but the new service expects non-null values
      return escapeCSVSeparator(
          transliterationService.transliterate(Strings.nullToEmpty(cityName)));
    }
    return escapeCSVSeparator(sis2TransformationService.transformAsString(cityName));
  }

  public String normalizePostalCode(final String postalCode) {
    return postalCode;
  }

  public String normalizeCity(String cityName) {
    return normalizeNameNoSpace(cityName);
  }

  public String normalizeStreet(String streetName) {
    return expandStr(normalizeNameNoSpace(streetName));
  }

  public String normalizeStreetNoExt(String streetNo) {
    return removeNull(normalizeNameNoSpace(streetNo));
  }

  /** Normalize all parts of the address and return a new object */
  @Nonnull
  public LookupTree.LookupRequest normalize(@Nonnull final LookupTree.LookupRequest address) {
    final String city = Optional.ofNullable(address.city()).map(this::normalizeCity).orElse(null);
    final String street =
        Optional.ofNullable(address.street()).map(this::normalizeStreet).orElse(null);
    final String no =
        Optional.ofNullable(address.no()).map(this::normalizeStreetNoExt).orElse(null);
    final String ext =
        Optional.ofNullable(address.ext()).map(this::normalizeStreetNoExt).orElse(null);
    return new LookupTree.LookupRequest(
        normalizePostalCode(address.postalCode()), city, street, no, ext);
  }
}
