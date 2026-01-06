package de.gematik.demis.nrs.service.futs;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.service.dto.HealthOfficeId;
import de.gematik.demis.service.base.error.ServiceCallException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TuberculosisConceptMapSupplierTest {

  @Mock private FutsClient futsClient;
  private TuberculosisConceptMapSupplier supplier;

  @BeforeEach
  public void setUp() {
    supplier = new TuberculosisConceptMapSupplier(futsClient);
  }

  @Test
  void thatStringsAreConvertedToHealthOfficeIds() {
    when(futsClient.getConceptMap(any()))
        .thenReturn(Map.of("1.2.3.4.5.", "1.", "1.", "1.2.3.4.5."));

    final Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
    assertThat(actual)
        .containsAllEntriesOf(
            Map.of(
                HealthOfficeId.from("1.2.3.4.5."),
                HealthOfficeId.RKI,
                HealthOfficeId.RKI,
                HealthOfficeId.from("1.2.3.4.5.")));
  }

  @Test
  void thatEmptyMapsAreHandled() {
    when(futsClient.getConceptMap(any())).thenReturn(Map.of());
    final Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
    assertThat(actual).isEmpty();
  }

  @Test
  void thatMalformedIdsAreHandled() {
    when(futsClient.getConceptMap(any())).thenReturn(Map.of("1.2.", "1.", "1.2.3.4.5.", "1."));
    final Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
    assertThat(actual)
        .containsExactly(Map.entry(HealthOfficeId.from("1.2.3.4.5."), HealthOfficeId.RKI));
  }

  @Nested
  class ApiExceptionHandling {
    @Test
    void thatAnEmptyMapIsReturnedOnInitialRequest() {
      when(futsClient.getConceptMap(any()))
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()));

      final Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
      assertThat(actual).isEmpty();
    }

    @Test
    void thatThePreviousResultIsReturnedOnSubsequentRequest() {
      // GIVEN
      when(futsClient.getConceptMap(any()))
          // an initial request that is successful
          .thenReturn(Map.of("1.2.3.4.5.", "1."))
          // AND the next request fails
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()));

      // WHEN a caller uses the API twice
      Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
      assertThat(actual)
          .containsExactly(Map.entry(HealthOfficeId.of(1, 2, 3, 4, 5), HealthOfficeId.RKI));

      // THEN we return the previous data
      actual = supplier.get();
      assertThat(actual)
          .containsExactly(Map.entry(HealthOfficeId.of(1, 2, 3, 4, 5), HealthOfficeId.RKI));
    }

    @Test
    void thatEventuallyAnEmptyMapIsReturnedForSubsequentExceptions() {
      final Map.Entry<HealthOfficeId, HealthOfficeId> successfulResult =
          Map.entry(HealthOfficeId.of(1, 2, 3, 4, 5), HealthOfficeId.RKI);

      // GIVEN
      when(futsClient.getConceptMap(any()))
          // an initial request that is successful
          .thenReturn(Map.of("1.2.3.4.5.", "1."))
          // AND the two subsequent requests that fail
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()))
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()));

      // WHEN a caller uses the API
      Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
      // THEN return the successful result
      assertThat(actual).containsExactly(successfulResult);
      // AND on exception return previously successful result
      actual = supplier.get();
      assertThat(actual).containsExactly(successfulResult);
      // AND on subsequent exception return empty map
      actual = supplier.get();
      assertThat(actual).isEmpty();
    }

    @Test
    void thatServiceRecoversFromSubsequentExceptions() {
      final Map.Entry<HealthOfficeId, HealthOfficeId> successfulResult =
          Map.entry(HealthOfficeId.of(1, 2, 3, 4, 5), HealthOfficeId.RKI);

      // GIVEN
      when(futsClient.getConceptMap(any()))
          // an initial request that is successful
          .thenReturn(Map.of("1.2.3.4.5.", "1."))
          // AND the two subsequent requests that fail
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()))
          .thenThrow(new ServiceCallException("any", "code", 500, new RuntimeException()))
          .thenReturn(Map.of("1.2.3.4.5.", "1."));

      // WHEN a caller uses the API
      Map<HealthOfficeId, HealthOfficeId> actual = supplier.get();
      // THEN return the successful result
      assertThat(actual).containsExactly(successfulResult);
      // AND on exception return previously successful result
      actual = supplier.get();
      assertThat(actual).containsExactly(successfulResult);
      // AND on subsequent exception return empty map
      actual = supplier.get();
      assertThat(actual).isEmpty();
      // AND finally we recover
      actual = supplier.get();
      assertThat(actual).containsExactly(successfulResult);
    }
  }
}
