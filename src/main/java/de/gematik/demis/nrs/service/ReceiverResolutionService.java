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
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 * #L%
 */

import de.gematik.demis.nrs.rules.model.RulesResultTypeEnum;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.HealthOfficeId;
import de.gematik.demis.nrs.service.futs.ConceptMapService;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.springframework.stereotype.Service;

/**
 * Translates abstract receiver information using routing data into a health office id
 *
 * <h3>Nomenclature</h3>
 *
 * <p>When translating addresses for tuberculosis receivers the following nomenclature is used: <br>
 *
 * <pre>address -> immediate health office -> tuberculosis health office.</pre>
 *
 * <br>
 * Where the immediate health office is the health office usually responsible for the given address.
 */
@Service
public class ReceiverResolutionService {

  private static final String LOOKUP_HEALTH_OFFICE_PREFIX_ID = "2";
  private static final String LOOKUP_HEALTH_OFFICE_DELIMITER = ".";

  @Nonnull private final AddressToHealthOfficeLookup addressToHealthOfficeLookup;

  @Nonnull private final ConceptMapService conceptMaps;

  /**
   * @param addressToHealthOfficeLookup
   * @param conceptMaps Access to the mapping of tuberculosis health offices.
   */
  public ReceiverResolutionService(
      @Nonnull final AddressToHealthOfficeLookup addressToHealthOfficeLookup,
      @Nonnull final ConceptMapService conceptMaps) {
    this.addressToHealthOfficeLookup = addressToHealthOfficeLookup;
    this.conceptMaps = conceptMaps;
  }

  @Nonnull
  public Optional<String> compute(
      @Nonnull final RulesResultTypeEnum receiverType, @Nonnull final AddressDTO address) {
    return switch (receiverType) {
      case SPECIFIC_RECEIVER ->
          throw new IllegalArgumentException("SPECIFIC_RECEIVER type is not supported");
      case RESPONSIBLE_HEALTH_OFFICE -> addressToHealthOfficeLookup.lookup(address);
      case RESPONSIBLE_HEALTH_OFFICE_SORMAS -> {
        final Optional<String> immediateHealthOffice = addressToHealthOfficeLookup.lookup(address);
        yield immediateHealthOffice.map(
            immediate ->
                LOOKUP_HEALTH_OFFICE_PREFIX_ID
                    + immediate.substring(immediate.indexOf(LOOKUP_HEALTH_OFFICE_DELIMITER)));
      }
      case RESPONSIBLE_HEALTH_OFFICE_TUBERCULOSIS -> {
        final Optional<String> immediateHealthOffice = addressToHealthOfficeLookup.lookup(address);
        // Standardize raw health office ids to their canonical form for reliable lookup and avoid
        // issues from typos or inconsistent formatting.
        yield immediateHealthOffice
            .map(HealthOfficeId::from)
            .flatMap(conceptMaps::tuberculosisHealthOfficeFor)
            .map(HealthOfficeId::getCanonicalRepresentation)
            .or(() -> immediateHealthOffice);
      }
      default -> Optional.empty();
    };
  }
}
