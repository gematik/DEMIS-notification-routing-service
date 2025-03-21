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

import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class Statistics {
  private static final String TIMER_CSV_LOOKUP = "address_csv_lookup.timer";
  private static final String COUNTER_CSV_LOOKUP = "address_csv_lookup";
  private static final String COUNTER_CSV_LOOKUP_TAG_MAP = "map";
  private static final String COUNTER_CSV_LOOKUP_TAG_STATUS = "status";

  private static final String COUNTER_NOT_GERMAN = "address_not_german";
  private static final String COUNTER_NOT_GERMAN_TAG_COUNTRY_GIVEN = "country_given";

  private static final String COUNTER_HEALTH_OFFICE_LOOKUP = "health_office_lookup";
  private static final String COUNTER_HEALTH_OFFICE_LOOKUP_TAG_ADDRESS_ORIGIN = "address_origin";
  private static final String COUNTER_HEALTH_OFFICE_LOOKUP_TAG_FOUND = "found";

  private static final String COUNTER_RESPONSIBLE = "responsible";
  private static final String COUNTER_RESPONSIBLE_TAG_ADDRESS_ORIGIN = "address_origin";

  private static final String COUNTER_NO_HEALTH_OFFICE_RESPONSIBLE = "no_health_office_responsible";

  private final MeterRegistry meterRegistry;

  public void recordLookup(
      final Duration duration, final String mapName, final String lookupStatus) {
    Timer.builder(TIMER_CSV_LOOKUP)
        .tags(COUNTER_CSV_LOOKUP_TAG_MAP, mapName, COUNTER_CSV_LOOKUP_TAG_STATUS, lookupStatus)
        .publishPercentileHistogram()
        .minimumExpectedValue(Duration.ofNanos(2_000))
        .maximumExpectedValue(Duration.ofNanos(10_000))
        .register(meterRegistry)
        .record(duration);
  }

  public void incCsvLookup(final String mapName, final String lookupStatus) {
    meterRegistry
        .counter(
            COUNTER_CSV_LOOKUP,
            COUNTER_CSV_LOOKUP_TAG_MAP,
            mapName,
            COUNTER_CSV_LOOKUP_TAG_STATUS,
            lookupStatus)
        .increment();
  }

  public void incNotGermanAddress(final boolean countryGiven) {
    meterRegistry
        .counter(
            COUNTER_NOT_GERMAN, COUNTER_NOT_GERMAN_TAG_COUNTRY_GIVEN, String.valueOf(countryGiven))
        .increment();
  }

  public void incHealthOfficeLookup(final AddressOriginEnum addressOrigin, final boolean found) {
    meterRegistry
        .counter(
            COUNTER_HEALTH_OFFICE_LOOKUP,
            COUNTER_HEALTH_OFFICE_LOOKUP_TAG_ADDRESS_ORIGIN,
            addressOrigin.name(),
            COUNTER_HEALTH_OFFICE_LOOKUP_TAG_FOUND,
            String.valueOf(found))
        .increment();
  }

  public void incResponsibleAddressOrigin(final AddressOriginEnum addressOriginEnum) {
    meterRegistry
        .counter(
            COUNTER_RESPONSIBLE, COUNTER_RESPONSIBLE_TAG_ADDRESS_ORIGIN, addressOriginEnum.name())
        .increment();
  }

  public void incNoHealthOfficeResponsible() {
    meterRegistry.counter(COUNTER_NO_HEALTH_OFFICE_RESPONSIBLE).increment();
  }
}
