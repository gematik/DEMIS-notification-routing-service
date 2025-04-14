package de.gematik.demis.nrs.rules.model;

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

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @param type can be responsible_health_office or specific_receiver
 * @param specificReceiverId the id of the specific receiver, if the
 * @param actions these action should be executed if a rule is applied
 * @param optional can be set to true for receiver, that don't have to receive and should not lead
 *     to an exception
 */
public record Route(
    @Nonnull RulesResultTypeEnum type,
    @Nullable String specificReceiverId,
    @Nonnull List<String> actions,
    boolean optional) {

  /** Match any Route with the given type */
  public static Predicate<Route> hasType(final RulesResultTypeEnum type) {
    return (route -> route.type().equals(type));
  }

  /** Match any route where the receiverId is null */
  public static Predicate<Route> receiverIsNull() {
    return (route -> Objects.isNull(route.specificReceiverId()));
  }

  /** Return a copy of this Route with the specified receiver id. */
  public Route copyWithReceiver(final String receiverId) {
    return new Route(type(), receiverId, actions(), optional());
  }
}
