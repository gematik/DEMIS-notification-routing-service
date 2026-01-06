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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TransliterationServiceTest {

  @MethodSource("conversionCandidates")
  @ParameterizedTest
  void doesCorrectTransliteration(final String input, final String expected) {
    final TransliterationService transliterationService = new TransliterationService();
    final String actual = transliterationService.transliterate(input);
    assertThat(actual).isEqualTo(expected);
  }

  /**
   * A list of addresses using special characters. The addresses are realy and illustrate the usage
   * of the special characters.
   */
  private static Stream<Arguments> conversionCandidates() {
    return Stream.of(
        Arguments.arguments("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        Arguments.arguments("abcdefghijklmnopqrstuvwxyz", "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        Arguments.arguments(
            "51647 Gummersbach S+C Wohnterrassen 1", "51647 GUMMERSBACH S+C WOHNTERRASSEN 1"),
        // Note the ',' here is on purpose!
        Arguments.arguments(
            "94474 Vilshofen Sandbach, Reisach 1", "94474 VILSHOFEN SANDBACH, REISACH 1"),
        Arguments.arguments(
            "98660 Themar M.-&-T. Werner Straße 1", "98660 THEMAR M.-&-T. WERNER STRASSE 1"),
        Arguments.arguments(
            "91578 Leutershausen William-O'Dwyer-Straße 1",
            "91578 LEUTERSHAUSEN WILLIAM-O'DWYER-STRASSE 1"),
        // We care about the brackets here
        Arguments.arguments(
            "79879 Wutach Außer Ort (Schwaningen) 13", "79879 WUTACH AUSSER ORT (SCHWANINGEN) 13"),
        // The extension here is using '/'
        Arguments.arguments(
            "99991 Unstrut-Hainich Obere Kirchstraße 30/32",
            "99991 UNSTRUT-HAINICH OBERE KIRCHSTRASSE 30/32"),
        // The extension here is using '_'
        Arguments.arguments(
            "14776 Brandenburg Sankt-Annen-Straße 30_36",
            "14776 BRANDENBURG SANKT-ANNEN-STRASSE 30_36"),
        Arguments.arguments(
            "86938 Schondorf St.-Jakob`s-Bergerl 1", "86938 SCHONDORF ST.-JAKOB`S-BERGERL 1"),
        Arguments.arguments("26607 Aurich An´t Burgschloot 15", "26607 AURICH AN´T BURGSCHLOOT 15"),
        Arguments.arguments(
            "47918 TÃ¶nisvorst Willicher Straße 95", "47918 TA¶NISVORST WILLICHER STRASSE 95"),
        Arguments.arguments(
            "84028 Landshut Altstadt 105 BUTTER¿", "84028 LANDSHUT ALTSTADT 105 BUTTER?"),
        Arguments.arguments("18147 Rostock Up’n Warnowsand 1", "18147 ROSTOCK UP'N WARNOWSAND 1"),
        Arguments.arguments(
            "74078 Heilbronn Franz-Lehár-Straße 38", "74078 HEILBRONN FRANZ-LEHAR-STRASSE 38"),
        Arguments.arguments(
            "75387 Neubulach Harry-à-Wengen-Straße 1", "75387 NEUBULACH HARRY-A-WENGEN-STRASSE 1"),
        Arguments.arguments(
            "06217 Merseburg Châtilloner Straße 1", "06217 MERSEBURG CHATILLONER STRASSE 1"),
        Arguments.arguments("äöüß", "AEOEUESS"),
        Arguments.arguments(
            "67655 Kaiserslautern Guimarães-Platz 1", "67655 KAISERSLAUTERN GUIMARAES-PLATZ 1"),
        Arguments.arguments("79111 Freiburg Besançonallee 1", "79111 FREIBURG BESANCONALLEE 1"),
        Arguments.arguments(
            "99734 Nordhausen Charleville-Mézières-Straße 1",
            "99734 NORDHAUSEN CHARLEVILLE-MEZIERES-STRASSE 1"),
        Arguments.arguments(
            "82256 Fürstenfeldbruck Almuñécarstraße 61",
            "82256 FUERSTENFELDBRUCK ALMUNECARSTRASSE 61"),
        Arguments.arguments("14055 Berlin Brontëweg 11", "14055 BERLIN BRONTEWEG 11"),
        Arguments.arguments(
            "04357 Leipzig Mockau-Nord Simón-Bolívar-Straße 99",
            "04357 LEIPZIG MOCKAU-NORD SIMON-BOLIVAR-STRASSE 99"),
        Arguments.arguments(
            "66802 Überherrn L'Hôpitaler Straße 2", "66802 UEBERHERRN L'HOPITALER STRASSE 2"),
        Arguments.arguments("25479 Ellerau Højerweg 9", "25479 ELLERAU HOJERWEG 9"),
        Arguments.arguments(
            "86842 Türkheim Vaskúter Straße 1", "86842 TUERKHEIM VASKUTER STRASSE 1"));
  }
}
