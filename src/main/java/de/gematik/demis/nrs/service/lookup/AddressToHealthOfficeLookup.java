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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Stopwatch;
import de.gematik.demis.nrs.service.Statistics;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.LookupMaps.LookupMap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AddressToHealthOfficeLookup {

  private static final LookupLevel[] CITY_BASED = {
    new LookupLevel(Detail.CITY, LookupMap.CITY),
    new LookupLevel(Detail.STREET, LookupMap.CITY_STREET),
    new LookupLevel(Detail.NUMBER, LookupMap.CITY_STREET_NO)
  };
  private static final LookupLevel[] POSTAL_CODE_BASED = {
    new LookupLevel(Detail.POSTAL_CODE, LookupMap.POSTALCODE, Fallback.newLookup(CITY_BASED)),
    new LookupLevel(
        Detail.CITY,
        LookupMap.POSTALCODE_CITY,
        Fallback.withPreviousKey(LookupMap.FALLBACK_POSTALCODE)),
    new LookupLevel(
        Detail.STREET,
        LookupMap.POSTALCODE_CITY_STREET,
        Fallback.withPreviousKey(LookupMap.FALLBACK_POSTALCODE_CITY)),
    new LookupLevel(
        Detail.NUMBER,
        LookupMap.POSTALCODE_CITY_STREET_NO,
        Fallback.withPreviousKey(LookupMap.FALLBACK_POSTALCODE_CITY_STREET))
  };

  private static final String MULTIPLE_MATCHES_KEYWORD = "many";

  private final LookupMaps lookupMaps;
  private final AddressNormalization normalizer;
  private final Statistics statistic;
  private final boolean isFuzzySearchEnabled;
  private final LookupTree lookupTree;
  private final boolean isSearchComparisonEnabled;

  public AddressToHealthOfficeLookup(
      final LookupMaps lookupMaps,
      final AddressNormalization normalizer,
      final LookupTree lookupTree,
      final Statistics statistic,
      @Value("${feature.flag.search.fuzzy}") final boolean isFuzzySearchEnabled,
      @Value("${feature.flag.search.comparison}") final boolean isSearchComparisonEnabled) {
    this.lookupMaps = lookupMaps;
    this.normalizer = normalizer;
    this.statistic = statistic;
    this.isFuzzySearchEnabled = isFuzzySearchEnabled;
    this.isSearchComparisonEnabled = isSearchComparisonEnabled;
    this.lookupTree = lookupTree;
  }

  public Optional<String> lookup(final AddressDTO address) {
    Objects.requireNonNull(address, "address is required");

    if (!address.isGerman()) {
      log.info("address not from germany: {}", address.countryCode());
      statistic.incNotGermanAddress(isNotBlank(address.countryCode()));
      return Optional.empty();
    }

    final Optional<String> exactMatchBasedResult = executeLookup(POSTAL_CODE_BASED, address);
    if (isSearchComparisonEnabled || isFuzzySearchEnabled) {
      final Optional<String> fuzzyMatchBasedResult = lookupWithFuzzySearchStrategy(address);
      boolean hasSameResult = fuzzyMatchBasedResult.equals(exactMatchBasedResult);
      if (isSearchComparisonEnabled) {
        final String comparisonResult =
            getComparisonResult(hasSameResult, exactMatchBasedResult, fuzzyMatchBasedResult);
        statistic.recordLookupComparison(hasSameResult, comparisonResult);
      }
      if (isFuzzySearchEnabled) {
        return fuzzyMatchBasedResult;
      } else {
        return exactMatchBasedResult;
      }
    }

    return exactMatchBasedResult;
  }

  @Nonnull
  private String getComparisonResult(
      final boolean hasSameResult,
      @Nonnull final Optional<String> exactMatchBasedResult,
      @Nonnull final Optional<String> fuzzyMatchBasedResult) {
    if (hasSameResult) {
      return Statistics.LOOKUP_COMPARISON_RESULT_IDENTICAL;
    }

    final boolean isFuzzySearchAdvantageous =
        exactMatchBasedResult.isEmpty() && fuzzyMatchBasedResult.isPresent();
    if (isFuzzySearchAdvantageous) {
      return Statistics.LOOKUP_COMPARISON_RESULT_FUZZY_WINS;
    }

    final boolean isExactSearchAdvantageous =
        exactMatchBasedResult.isPresent() && fuzzyMatchBasedResult.isEmpty();
    if (isExactSearchAdvantageous) {
      return Statistics.LOOKUP_COMPARISON_RESULT_EXACT_WINS;
    }

    return Statistics.LOOKUP_COMPARISON_RESULT_BOTH_DIFFER;
  }

  private Optional<String> lookupWithFuzzySearchStrategy(final AddressDTO address) {
    final Stopwatch stopwatch = Stopwatch.createStarted();
    LookupTree.LookupRequest lookupRequest = LookupTree.LookupRequest.from(address);
    lookupRequest = normalizer.normalize(lookupRequest);
    final Optional<LookupTree.LookupResult> lookupResult =
        lookupTree.lookupHealthOffice(lookupRequest);
    final Optional<String> result = lookupResult.map(LookupTree.LookupResult::healthOffice);
    stopwatch.stop();
    statistic.recordLookup(stopwatch.elapsed(), lookupResult);
    return result;
  }

  private Optional<String> executeLookup(
      final LookupLevel[] detailLevels, final AddressDTO address) {
    final LookupResult lookupResult = iterativeLookup(detailLevels, address);

    if (lookupResult.status() != LookupStatus.UNIQUE_MATCH) {
      final Fallback fallback = lookupResult.detailLevel().fallback();
      if (fallback != null) {
        return fallback.executeFallback(this, address, lookupResult.keyParts());
      }
    }

    return lookupResult.toOptional();
  }

  private LookupResult iterativeLookup(final LookupLevel[] detailLevels, final AddressDTO address) {
    final List<String> keyParts = new ArrayList<>();

    for (final LookupLevel currentLevel : detailLevels) {
      final Stopwatch timer = Stopwatch.createStarted();
      final String detailValue = getAddressDetailValue(address, currentLevel.additionalDetail());
      keyParts.add(detailValue);

      final LookupStatus status;
      final String result;

      if (detailValue == null) {
        result = null;
        status = LookupStatus.DETAIL_NOT_PROVIDED;
      } else {
        result = getHealthOffice(currentLevel.map(), keyParts);

        if (result == null) {
          status = LookupStatus.NOT_FOUND;
        } else if (MULTIPLE_MATCHES_KEYWORD.equalsIgnoreCase(result)) {
          status = LookupStatus.MULTIPLE_MATCHES;
        } else {
          status = LookupStatus.UNIQUE_MATCH;
        }
      }

      final Duration elapsed = timer.elapsed();
      logCsvLookup(elapsed, currentLevel.map, status);

      if (status != LookupStatus.MULTIPLE_MATCHES) {
        return new LookupResult(status, result, currentLevel, keyParts);
      }
    }

    throw new IllegalStateException("no next lookup level!");
  }

  private void logCsvLookup(
      final Duration elapsed, final LookupMap map, final LookupStatus status) {
    log.debug("lookup map {} -> {}", map.name(), status.name());
    statistic.incCsvLookup(map.name(), status.name());
    statistic.recordLookup(elapsed, map.name(), status.name());
  }

  private String getHealthOffice(final LookupMap map, final List<String> addressKey) {
    return lookupMaps.getValue(map, addressKey.toArray(new String[0]));
  }

  private String getAddressDetailValue(final AddressDTO address, final Detail detail) {
    String value = detail.getValueSupplier().apply(address);
    if (isNotBlank(value)) {
      value = detail.getNormalizeFunction().apply(normalizer, value);
    }
    if (StringUtils.isBlank(value)) {
      value = null;
    }
    return value;
  }

  enum LookupStatus {
    DETAIL_NOT_PROVIDED,
    NOT_FOUND,
    UNIQUE_MATCH,
    MULTIPLE_MATCHES
  }

  @Getter
  @RequiredArgsConstructor
  private enum Detail {
    POSTAL_CODE(AddressDTO::postalCode, AddressNormalization::normalizePostalCode),
    CITY(AddressDTO::city, AddressNormalization::normalizeCity),
    STREET(AddressDTO::street, AddressNormalization::normalizeStreet),
    NUMBER(AddressDTO::no, AddressNormalization::normalizeStreetNoExt);

    private final Function<AddressDTO, String> valueSupplier;
    private final BiFunction<AddressNormalization, String, String> normalizeFunction;
  }

  @FunctionalInterface
  private interface Fallback {
    static Fallback withPreviousKey(final LookupMap otherLookupMap) {
      return (lookupEngine, notNeeded, key) -> {
        final Stopwatch timer = Stopwatch.createStarted();
        final List<String> previousKey = key.subList(0, key.size() - 1);
        final String healthOffice = lookupEngine.getHealthOffice(otherLookupMap, previousKey);
        final Duration elapsed = timer.elapsed();
        lookupEngine.logCsvLookup(
            elapsed,
            otherLookupMap,
            healthOffice == null ? LookupStatus.NOT_FOUND : LookupStatus.UNIQUE_MATCH);
        return Optional.ofNullable(healthOffice);
      };
    }

    static Fallback newLookup(final LookupLevel[] newLookupLevels) {
      return (lookupEngine, address, key) -> lookupEngine.executeLookup(newLookupLevels, address);
    }

    Optional<String> executeFallback(
        AddressToHealthOfficeLookup lookupEngine, AddressDTO address, List<String> key);
  }

  private record LookupResult(
      LookupStatus status, String result, LookupLevel detailLevel, List<String> keyParts) {
    public Optional<String> toOptional() {
      return status == LookupStatus.UNIQUE_MATCH ? Optional.of(result) : Optional.empty();
    }
  }

  private record LookupLevel(Detail additionalDetail, LookupMap map, Fallback fallback) {
    public LookupLevel(Detail additionalDetail, LookupMap map) {
      this(additionalDetail, map, null);
    }
  }
}
