package de.gematik.demis.nrs.api;

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

import static de.gematik.demis.nrs.service.dto.AddressDTO.COUNTRY_CODE_GERMANY;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import de.gematik.demis.nrs.api.dto.RoutingOutput;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTO;
import de.gematik.demis.nrs.api.dto.RuleBasedRouteDTOPreBundleAction;
import de.gematik.demis.nrs.service.NotificationRoutingService;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.lookup.AddressToHealthOfficeLookup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class NotificationRoutingController {

  private final NotificationRoutingService notificationRoutingService;
  private final AddressToHealthOfficeLookup healthOfficeLookupService;
  private final boolean is73RoutingEnabled;
  private final boolean is74RoutingEnabled;

  public NotificationRoutingController(
      final NotificationRoutingService notificationRoutingService,
      final AddressToHealthOfficeLookup healthOfficeLookupService,
      @Value("${feature.flag.notifications.7_3}") final boolean is73RoutingEnabled,
      @Value("${feature.flag.notifications.7_4}") final boolean is74RoutingEnabled) {
    this.notificationRoutingService = notificationRoutingService;
    this.healthOfficeLookupService = healthOfficeLookupService;
    this.is73RoutingEnabled = is73RoutingEnabled;
    this.is74RoutingEnabled = is74RoutingEnabled;
  }

  @PostMapping(
      path = "/routing",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public RoutingOutput determineRouting(@RequestBody final String fhirNotification) {
    return notificationRoutingService.determineRouting(fhirNotification);
  }

  @PostMapping(
      path = "/routing/v2",
      consumes = APPLICATION_JSON_VALUE,
      produces = APPLICATION_JSON_VALUE)
  public Object determineRuleBasedRouting(
      @RequestBody final String fhirNotification,
      @RequestParam("isTestUser") final boolean isTestUser,
      @RequestParam("testUserID") final String sender) {
    final RuleBasedRouteDTO bundleActionBasedRouting =
        notificationRoutingService.determineRuleBasedRouting(fhirNotification, isTestUser, sender);
    // Once EITHER flag is removed, the other can be removed too
    if (is73RoutingEnabled || is74RoutingEnabled) {
      return bundleActionBasedRouting;
    }
    return RuleBasedRouteDTOPreBundleAction.from(bundleActionBasedRouting);
  }

  @GetMapping("/routing/health-office")
  public String findHealthOfficeByAddress(
      @RequestParam(required = false) final String street,
      @RequestParam(required = false) final String no,
      @RequestParam(required = false) final String postalCode,
      @RequestParam(required = false) final String city,
      @RequestParam(required = false, defaultValue = COUNTRY_CODE_GERMANY)
          final String countryCode) {
    final AddressDTO addressDTO = new AddressDTO(street, no, postalCode, city, countryCode);
    return healthOfficeLookupService.lookup(addressDTO).orElse("");
  }
}
