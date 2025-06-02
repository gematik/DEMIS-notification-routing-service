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

import com.ibm.icu.text.Transliterator;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Service;

/**
 * Provide means to normalize incoming address strings to remove ambiguity and improve matching
 * precision. This is based on a previous implementation. To avoid a bigger refactoring, this
 * service will also convert input to upper-case which isn't strictly transliteration.
 */
@Service
public class TransliterationService {

  /**
   * Any-Latin ensures all kinds of characters are first made into Latin (e.g. Cyrillic into Latin),
   * then we use de-ASCII to convert remaining special characters into oe, ae, etc. There is
   * Latin-ASCII, but that will convert ä -> a instead of ae.
   */
  private static final Transliterator TRANSLITERATOR =
      Transliterator.getInstance("Any-Latin;de-ASCII;Upper");

  /** Transliterate the given input */
  @Nonnull
  public String transliterate(@Nonnull final String input) {
    return TRANSLITERATOR.transliterate(input);
  }
}
