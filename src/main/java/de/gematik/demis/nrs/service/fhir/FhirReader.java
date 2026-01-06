package de.gematik.demis.nrs.service.fhir;

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

import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_CURRENT;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_ORDINARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_OTHER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIED_PERSON_PRIMARY;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.NOTIFIER;
import static de.gematik.demis.nrs.api.dto.AddressOriginEnum.SUBMITTER;

import ca.uhn.fhir.parser.DataFormatException;
import de.gematik.demis.fhirparserlibrary.FhirParser;
import de.gematik.demis.fhirparserlibrary.MessageType;
import de.gematik.demis.nrs.api.dto.AddressOriginEnum;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.DestinationLookupReaderInput;
import de.gematik.demis.nrs.service.dto.RoutingInput;
import de.gematik.demis.service.base.error.ServiceException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FhirReader {

  private final FhirParser fhirParser;
  private final AddressResolver addressResolver;
  private final AddressMapper mapper;

  private static AddressOriginEnum toNotifiedPersonAddressOrigin(final AddressUseEnum addressUse) {
    return switch (addressUse) {
      case CURRENT -> NOTIFIED_PERSON_CURRENT;
      case ORDINARY -> NOTIFIED_PERSON_ORDINARY;
      case PRIMARY -> NOTIFIED_PERSON_PRIMARY;
      default -> NOTIFIED_PERSON_OTHER;
    };
  }

  public RoutingInput extractRoutingInput(final String bundleAsJsonString) {
    return getRoutingInput(toBundle(bundleAsJsonString));
  }

  public RoutingInput getRoutingInput(final Bundle bundle) {
    final var bundleResources = new BundleResourceProvider(bundle);
    log.info("Processing notification bundle with id {}", bundleResources.getIdentifier());
    return new RoutingInput(createAddressMap(bundleResources));
  }

  private Map<AddressOriginEnum, AddressDTO> createAddressMap(
      final BundleResourceProvider bundleResources) {
    return new AddressMapBuilder()
        .add(NOTIFIER, bundleResources.getNotifierRole(), addressResolver::fromPractitionerRole)
        // TODO Nur bei Labor. Conditional?
        .add(SUBMITTER, bundleResources.getSubmittingRole(), addressResolver::fromPractitionerRole)
        .addList(
            FhirReader::toNotifiedPersonAddressOrigin,
            bundleResources.getNotifiedPerson(),
            addressResolver::fromPatient)
        .build();
  }

  public Bundle toBundle(final String json) {
    try {
      return fhirParser.parseBundleOrParameter(json, MessageType.JSON);
    } catch (final DataFormatException ex) {
      throw new ServiceException(HttpStatus.BAD_REQUEST, null, "Error parsing notification", ex);
    }
  }

  /**
   * Receives a bundle and extracts DestinationLookupReaderInput
   *
   * @param bundle the FHIR bundle containing the data to be evaluated.
   * @return DestinationLookupReaderInput with notificationId and notificationCategory from bundle
   *     or null if information in bundle is not present
   */
  public Optional<DestinationLookupReaderInput> getDestinationLookupReaderInformation(
      final Bundle bundle, String notificationType) {
    final BundleResourceProvider bundleResources = new BundleResourceProvider(bundle);
    final String notificationId = bundleResources.getRelatesToNotificationId();
    final String notificationCategory = bundleResources.getNotificationCategory(notificationType);
    if (notificationId != null && notificationCategory != null) {
      return Optional.of(new DestinationLookupReaderInput(notificationId, notificationCategory));
    }
    return Optional.empty();
  }

  private class AddressMapBuilder {
    final Map<AddressOriginEnum, AddressDTO> addresses = new EnumMap<>(AddressOriginEnum.class);

    public <R> AddressMapBuilder add(
        final AddressOriginEnum type,
        Optional<R> resource,
        final Function<R, Optional<Address>> getAddress) {
      resource.flatMap(getAddress).ifPresent(a -> addEntry(type, a));
      return this;
    }

    public <R> AddressMapBuilder addList(
        final Function<AddressUseEnum, AddressOriginEnum> useToOriginEnumMapper,
        final Optional<R> resource,
        final Function<R, Map<AddressUseEnum, Address>> getAddresses) {
      resource
          .map(getAddresses)
          .orElse(Map.of())
          .forEach((use, address) -> addEntry(useToOriginEnumMapper.apply(use), address));
      return this;
    }

    private void addEntry(final AddressOriginEnum type, final Address address) {
      addresses.put(type, mapper.toDto(address));
    }

    public Map<AddressOriginEnum, AddressDTO> build() {
      return Collections.unmodifiableMap(addresses);
    }
  }
}
