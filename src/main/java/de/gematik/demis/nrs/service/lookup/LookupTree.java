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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import de.gematik.demis.nrs.service.dto.AddressDTO;
import de.gematik.demis.nrs.service.dto.LookupAddress;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.apache.commons.text.similarity.LevenshteinDistance;

/**
 * Rudimentary tree structure with levels based on parts of an address. We assume that address parts
 * form a hierarchy, i.e. a postal code contains cities, which contain streets etc.
 */
public class LookupTree {

  /**
   * While the root nodes are referenced using an {@link ImmutableMap}, remember that {@link
   * Node#children()} is currently using a mutable Map. Changing these to an immutable version,
   * would require an expensive walk of the entire tree upon construction. We keep the {@link Node}
   * class hidden to avoid accidental mutations of the underlying data.
   */
  private final ImmutableMap<String, Node> rootNodes;

  /** Prefer {@link Builder} to create a new instance. */
  private LookupTree(@Nonnull final Map<String, Node> rootNodes) {
    this.rootNodes = ImmutableMap.copyOf(rootNodes);
  }

  /**
   * A helper class that builds an immutable {@link LookupTree} data structure. Note: you can only
   * call build once on this instance. It is not possible to call build more than once, because
   * adding to the tree after a build will change the underlying data structure.
   */
  public static class Builder {

    private static final String ILLEGAL_STATE_MESSAGE =
        "This instance has already been used to build. You can no longer use it.";

    private Builder() {}

    private final Map<String, Node> rootNodes = new HashMap<>();
    private boolean isSealed = false;

    /**
     * Create a new node among ancestors if non exists. Update existing node if we know the health
     * office and there are no more child nodes.
     */
    @Nonnull
    private Node addNode(
        @Nonnull final Map<String, Node> ancestors,
        @Nonnull final String nodeValue,
        boolean hasNoMoreChildren,
        @Nonnull final LookupAddress lookupAddress,
        @Nonnull final Level level) {
      Node result =
          ancestors.computeIfAbsent(
              nodeValue,
              s -> {
                String ga = null;
                if (hasNoMoreChildren) {
                  ga = lookupAddress.getHealthOffice();
                }
                return new Node(nodeValue, new HashMap<>(), ga, level);
              });

      // Update the current node's health office, if we have no more children recorded and are
      // missing one on this level
      // i.e. we already have added a more precise address, but are lacking the right information
      // for this one
      if (hasNoMoreChildren && result.healthOffice() == null) {
        result =
            ancestors.computeIfPresent(
                nodeValue,
                (key, node) -> node.replaceHealthOffice(lookupAddress.getHealthOffice()));
      }
      Objects.requireNonNull(result);

      return result;
    }

    /**
     * @exception IllegalStateException if the build method of this instance has been called before
     */
    @Nonnull
    public Builder add(@Nonnull final LookupAddress lookupAddress) {
      checkIsNotSealed();

      boolean isLastNode = lookupAddress.getCity().isEmpty();
      final Node zipNode =
          addNode(
              rootNodes,
              lookupAddress.getPostalCode(),
              isLastNode,
              lookupAddress,
              Level.POSTAL_CODE);
      if (isLastNode) {
        return this;
      }

      isLastNode = lookupAddress.getStreet().isEmpty();
      final Node cityNode =
          this.addNode(
              zipNode.children(),
              lookupAddress.getCity().get(),
              isLastNode,
              lookupAddress,
              Level.CITY);
      if (isLastNode) {
        return this;
      }

      isLastNode = lookupAddress.getNumber().isEmpty();
      final Node streetNode =
          this.addNode(
              cityNode.children(),
              lookupAddress.getStreet().get(),
              isLastNode,
              lookupAddress,
              Level.STREET);
      if (isLastNode) {
        return this;
      }

      isLastNode = lookupAddress.getExtension().isEmpty();
      final Node numberNode =
          this.addNode(
              streetNode.children(),
              lookupAddress.getNumber().get(),
              isLastNode,
              lookupAddress,
              Level.NUMBER);
      if (isLastNode) {
        return this;
      }

      final String extension = lookupAddress.getExtension().get();
      numberNode
          .children()
          .put(extension, Node.forExtension(extension, lookupAddress.getHealthOffice()));
      return this;
    }

    private void checkIsNotSealed() {
      if (isSealed) {
        throw new IllegalStateException(ILLEGAL_STATE_MESSAGE);
      }
    }

    /**
     * @return a new immutable {@link LookupTree}
     * @exception IllegalStateException if the build method of this instance has been called before
     */
    @Nonnull
    public LookupTree build() {
      checkIsNotSealed();

      this.isSealed = true;
      return new LookupTree(rootNodes);
    }
  }

  @Nonnull
  public static LookupTree.Builder builder() {
    return new Builder();
  }

  /** Signifies the level of a {@link Node} in the {@link LookupTree}. */
  public enum Level {
    POSTAL_CODE(),
    CITY(),
    STREET(),
    NUMBER(),
    EXTENSION();

    private static final Level[] cachedValues = values();

    /**
     * @return the next rank based on ordinal order
     * @throws IllegalStateException if there is no next rank
     */
    @Nonnull
    public Level next() {
      final int nextRank = this.ordinal() + 1;
      if (nextRank >= Level.cachedValues.length) {
        throw new IllegalStateException("There is no next rank after " + this.name());
      }
      return Level.cachedValues[nextRank];
    }
  }

  /** An immutable value object to perform searches through a {@link LookupTree}. */
  public record LookupRequest(
      @Nonnull String postalCode,
      @CheckForNull String city,
      @CheckForNull String street,
      @CheckForNull String no,
      @CheckForNull String ext) {
    public LookupRequest {
      /*
       Empty strings are converted to null for nullable fields, so that we can use Optionals to
       check whether we have to refine our search.
      */
      city = Strings.emptyToNull(city);
      street = Strings.emptyToNull(street);
      no = Strings.emptyToNull(no);
      ext = Strings.emptyToNull(ext);
    }

    @Nonnull
    public static LookupRequest from(@Nonnull final AddressDTO address) {
      return new LookupRequest(
          address.postalCode(), address.city(), address.street(), address.no(), null);
    }

    /** Return data associated with the given level */
    @Nonnull
    private Optional<String> getLevel(@Nonnull final Level level) {
      return switch (level) {
        case POSTAL_CODE -> Optional.of(postalCode);
        case CITY -> Optional.ofNullable(city);
        case STREET -> Optional.ofNullable(street);
        case NUMBER -> Optional.ofNullable(no);
        case EXTENSION -> Optional.ofNullable(ext);
      };
    }
  }

  private static int maxEditDistanceForChildren(@Nonnull final Level parentLevel) {
    return switch (parentLevel) {
      case POSTAL_CODE, CITY:
        yield 2; // 2 so we can handle swaps
      default:
        yield 0;
    };
  }

  /**
   * A perfect match is a match where we can find a GA using some of the data from the search and
   * the edit distance being 0.
   */
  @Nonnull
  public Optional<LookupResult> lookupHealthOffice(@Nonnull final LookupRequest search) {
    final SearchCandidateComparator byPrecisionThenEditDistance = new SearchCandidateComparator();
    final PriorityQueue<SearchCandidate> searchAnchors =
        new PriorityQueue<>(byPrecisionThenEditDistance);

    final String postalCode = search.postalCode();
    final Node root = rootNodes.get(postalCode);
    if (root == null) {
      return Optional.empty();
    }

    searchAnchors.add(SearchCandidate.from(root));
    return search(search, searchAnchors, byPrecisionThenEditDistance);
  }

  /**
   * Iteratively search for the best candidate using a priority queue.
   *
   * @param searchData The data we want to use for searching
   * @param candidateQueue The nodes that we still want to traverse
   * @param score The same comparator that is used for the candidateQueue
   */
  @Nonnull
  private Optional<LookupResult> search(
      @Nonnull final LookupRequest searchData,
      @Nonnull final Queue<SearchCandidate> candidateQueue,
      @Nonnull final Comparator<SearchCandidate> score) {
    // This avoids null checks on `result`. The first element on the queue will
    // usually be the postal code node. It has an edit distance of 0. We need to
    // design our score function in such a way that more precise results with
    // larger edit distance can supersede this postal code node. Otherwise, a
    // more precise result can probably be achieved by (1) setting `result = null`
    // and (2) changing the score function to prefer lower edit distance and
    // prioritise precision.
    SearchCandidate result = candidateQueue.peek();

    while (!candidateQueue.isEmpty()) {
      final SearchCandidate candidateProxy = candidateQueue.poll();
      if (score.compare(candidateProxy, result) > 0) {
        // The queue is sorted by edit distance. We can't improve our result, once the item with the
        // smallest edit distance on the queue has a larger edit distance than our result.
        break;
      }

      final Node candidate = candidateProxy.node();
      boolean isLeaf = candidate.children().isEmpty() || candidate.healthOffice() != null;
      final boolean candidateImprovesResult = score.compare(candidateProxy, result) < 0;
      if (isLeaf && candidateImprovesResult) {
        result = candidateProxy;
      }

      if (candidate.children().isEmpty()) {
        // Stop adding more candidates to the queue, when there is no more children.
        continue;
      }

      // Try and add more candidates to find a more precise candidate, this is useful if we search
      // for a,b and we know
      // a -> 1
      // a,b -> 2
      // if we stop at 'a', we are in the right vicinity, but it's not the best possible match
      final Optional<String> nextSearchTerm = searchData.getLevel(candidate.level().next());
      if (nextSearchTerm.isEmpty()) {
        // We can't go deeper because we are missing data in our search input, but perhaps we have a
        // result candidate that we can use
        continue;
      }

      final int allowedEditDistance = maxEditDistanceForChildren(candidate.level());
      final boolean requiresExactMatch = allowedEditDistance == 0;
      if (requiresExactMatch) {
        final Node nextCandidate = candidate.children.get(nextSearchTerm.get());
        if (nextCandidate != null) {
          candidateQueue.add(candidateProxy.passOnEditDistance(nextCandidate, 0));
        }

        // We need to find a perfect match so we stop this iteration, otherwise similar candidates
        // would be added
        continue;
      }

      final Map<Node, Integer> nextCandidates =
          retainBelowEditDistance(nextSearchTerm.get(), candidate.children(), allowedEditDistance);
      for (final Map.Entry<Node, Integer> nextCandidate : nextCandidates.entrySet()) {
        final SearchCandidate searchCandidate =
            candidateProxy.passOnEditDistance(nextCandidate.getKey(), nextCandidate.getValue());
        candidateQueue.add(searchCandidate);
      }
    }

    return LookupResult.ofNullable(result);
  }

  @Nonnull
  private Map<Node, Integer> retainBelowEditDistance(
      @Nonnull final String searchTerm,
      @Nonnull final Map<String, Node> candidates,
      final int maxEditDistance) {
    if (maxEditDistance <= 0) {
      throw new IllegalArgumentException("Provide a max edit distance of 1 or higher");
    }

    final LevenshteinDistance defaultInstance = new LevenshteinDistance(maxEditDistance);
    final ImmutableMap.Builder<Node, Integer> builder = ImmutableMap.builder();
    for (final Node node : candidates.values()) {
      // editDistance will be -1 if above threshold
      final Integer editDistance = defaultInstance.apply(node.value, searchTerm);
      if (editDistance > -1 && editDistance <= maxEditDistance) {
        builder.put(node, editDistance);
      }
    }
    return builder.build();
  }

  /**
   * Keeps track of the edit distance for a node in our search
   *
   * @param editDistance
   * @param node
   */
  public record SearchCandidate(int editDistance, @Nonnull Node node) {
    /** Initialize a new search candidate and set the edit distance to 0 */
    @Nonnull
    private static SearchCandidate from(@Nonnull final Node node) {
      return new SearchCandidate(0, node);
    }

    /** Return a new search candidate and add the edit distances together */
    @Nonnull
    private SearchCandidate passOnEditDistance(
        @Nonnull final Node nextCandidate, final int additionalEditDistance) {
      return new SearchCandidate(additionalEditDistance + editDistance(), nextCandidate);
    }
  }

  /**
   * Keeps track of the basic information that make up the tree
   *
   * @param value
   * @param children
   * @param healthOffice
   * @param level
   */
  public record Node(
      @Nonnull String value,
      @Nonnull Map<String, Node> children,
      @CheckForNull String healthOffice,
      @Nonnull Level level) {

    @Nonnull
    private static Node forExtension(@Nonnull String value, @Nonnull String healthOffice) {
      return new Node(value, Map.of(), healthOffice, Level.EXTENSION);
    }

    @Nonnull
    public Node replaceHealthOffice(@Nonnull final String newHealthOffice) {
      return new Node(this.value, this.children, newHealthOffice, this.level);
    }
  }

  public record LookupResult(@Nonnull String healthOffice, int editDistance, @Nonnull Level level) {

    @Nonnull
    private static Optional<LookupResult> ofNullable(@CheckForNull final SearchCandidate result) {
      if (result == null || result.node().healthOffice() == null) {
        return Optional.empty();
      }
      return Optional.of(
          new LookupResult(
              result.node().healthOffice(), result.editDistance(), result.node().level()));
    }
  }
}
