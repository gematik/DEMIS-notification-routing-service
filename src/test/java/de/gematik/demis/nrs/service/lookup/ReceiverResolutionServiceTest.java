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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.when;

import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.ReceiverResolutionService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.HealthOfficeId;
import de.gematik.demis.nrs.service.futs.ConceptMapService;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReceiverResolutionServiceTest {

  public static final AddressDTO ADDRESS = new AddressDTO("Any", "No", "12345", "City", "DE");
  @Mock private AddressToHealthOfficeLookup addressToHealthOfficeLookup;
  @Mock private ConceptMapService conceptMapService;

  @Test
  void thatSpecificReceiverIsNotSupported() {
    /*
    SpecificReceiver requires us to set the id that comes with the Route. This is very easy to do. However, given
    that id is nullable we would either a) need to provide an interface with three parameters where in most cases one
    is null or b) use Route as parameter which is harder to construct for callers (e.g. they'd need to provide actions
    and understand what optional is supposed to do). Yet we still would either have to throw an IllegalArgumentException
     or NPE in case the caller provides SPECIFIC_RECEIVER and no specificReceiverId, which would be permissible due to
     the interface. A third option c) would be to split into two methods but at that point it's easier for the caller
     to do it themselves.
     */
    final ReceiverResolutionService receiverResolutionService =
        new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () ->
                receiverResolutionService.compute(RulesResultTypeEnum.SPECIFIC_RECEIVER, ADDRESS));
  }

  @Nested
  class HealthOfficeTests {

    @Test
    void thatAddressFromLookupServiceIsReturned() {
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.of("1.2.3.4.5"));

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      final Optional<String> actual =
          receiverResolutionService.compute(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE, ADDRESS);
      assertThat(actual).contains("1.2.3.4.5");
    }

    @Test
    void thatEmptyResultIsReturnedForUnknownAddress() {
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.empty());

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      final Optional<String> actual =
          receiverResolutionService.compute(RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE, ADDRESS);
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  class SomasTests {

    @Test
    void thatFirstPartIsReplacedBy2() {
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.of("?.X.X.X.X"));

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      final Optional<String> actual =
          receiverResolutionService.compute(
              RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS, ADDRESS);
      assertThat(actual).contains("2.X.X.X.X");
    }

    @Test
    void thatEmptyIsReturnedIfNoImmediateHealthOfficeIsFound() {
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.empty());

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      final Optional<String> actual =
          receiverResolutionService.compute(
              RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_SORMAS, ADDRESS);
      assertThat(actual).isEmpty();
    }
  }

  @Nested
  class TuberculosisTests {

    @Test
    void thatTuberculosisReceiverCanBeResolved() {
      // GIVEN a responsible health office '9.'
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.of("9."));
      // AND a responsible tuberculosis health office '99.'
      when(conceptMapService.tuberculosisHealthOfficeFor(HealthOfficeId.of(9)))
          .thenReturn(Optional.of(HealthOfficeId.of(99)));

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      // WHEN I compute the health office recipient
      final Optional<String> receiver =
          receiverResolutionService.compute(
              RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS, ADDRESS);

      // THEN the tuberculosis health office is returned
      assertThat(receiver).contains("99.");
    }

    @Test
    void thatImmediateIsReturnedIfTuberculosisCantBeResolved() {
      // GIVEN a responsible health office 'A'
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.of("9."));

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      // WHEN I compute the health office recipient
      final Optional<String> receiver =
          receiverResolutionService.compute(
              RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS, ADDRESS);

      // THEN the original health office is returned
      assertThat(receiver).contains("9.");
    }

    @Test
    void thatEmptyIsReturnedIfNothingCanBeResolved() {
      // GIVEN a responsible health office 'A'
      when(addressToHealthOfficeLookup.lookup(ADDRESS)).thenReturn(Optional.empty());

      final ReceiverResolutionService receiverResolutionService =
          new ReceiverResolutionService(addressToHealthOfficeLookup, conceptMapService);

      // WHEN I compute the health office recipient
      final Optional<String> receiver =
          receiverResolutionService.compute(
              RulesResultTypeEnum.RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS, ADDRESS);

      // THEN the original health office is returned
      assertThat(receiver).isEmpty();
    }
  }
}
