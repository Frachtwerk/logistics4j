/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of logistics4j.
 *
 * logistics4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logistics4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with logistics4j. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.dtd4j;

import de.frachtwerk.dtd4j.Particle.Choice;
import de.frachtwerk.dtd4j.Particle.Name;
import de.frachtwerk.dtd4j.Particle.Sequence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Matches the names of child elements against an element content model and keeps the derivation:
 * which occurrence of which particle each child belongs to. Each occurrence takes as many children
 * as it can; a repeated group starts a new occurrence only where the previous one cannot go on.
 */
final class ContentMatcher {

  /** A particle and what each of its occurrences matched. */
  record Part(Particle particle, List<Unit> occurrences) {}

  /** One occurrence of a particle. */
  sealed interface Unit {}

  /** A {@link Name} matched the child at {@code index}. */
  record Element(int index) implements Unit {}

  /** A {@link Sequence} matched one part per item. */
  record Items(List<Part> parts) implements Unit {}

  /** A {@link Choice} matched one of its options. */
  record Option(Part part) implements Unit {}

  private record Key(Particle particle, int pos) {}

  private record Result<T>(int end, T value) {}

  private final List<String> names;
  private final Map<Key, List<Result<Part>>> parts = new HashMap<>();

  private ContentMatcher(List<String> names) {
    this.names = names;
  }

  /** The derivation of all children, or empty if they do not match. */
  static Optional<Part> match(Particle particle, List<String> names) {
    ContentMatcher matcher = new ContentMatcher(names);
    return matcher.part(particle, 0).stream()
        .filter(result -> result.end() == names.size())
        .map(Result::value)
        .findFirst();
  }

  /** Every end position the particle can reach from {@code pos}, longest first, one each. */
  private List<Result<Part>> part(Particle particle, int pos) {
    Key key = new Key(particle, pos);
    List<Result<Part>> cached = parts.get(key);
    if (cached != null) {
      return cached;
    }
    Map<Integer, Result<Part>> byEnd = new LinkedHashMap<>();
    Occurs occurs = particle.occurs();
    if (occurs.optional()) {
      byEnd.put(pos, new Result<>(pos, new Part(particle, List.of())));
    }
    // chains of occurrences, one per end position; a further occurrence must consume children
    List<Result<List<Unit>>> chains = List.of(new Result<>(pos, List.of()));
    for (int count = 1; !chains.isEmpty(); count++) {
      Map<Integer, Result<List<Unit>>> next = new LinkedHashMap<>();
      for (Result<List<Unit>> chain : chains) {
        for (Result<Unit> unit : unit(particle, chain.end())) {
          if (count > 1 && unit.end() == chain.end()) {
            continue;
          }
          List<Unit> occurrences = new ArrayList<>(chain.value());
          occurrences.add(unit.value());
          next.putIfAbsent(unit.end(), new Result<>(unit.end(), occurrences));
        }
      }
      for (Result<List<Unit>> chain : next.values()) {
        // fewer occurrences win: each occurrence takes as many children as it can
        byEnd.putIfAbsent(
            chain.end(), new Result<>(chain.end(), new Part(particle, chain.value())));
      }
      if (!occurs.repeatable()) {
        break;
      }
      chains = List.copyOf(next.values());
    }
    List<Result<Part>> results = new ArrayList<>(byEnd.values());
    results.sort((a, b) -> Integer.compare(b.end(), a.end()));
    parts.put(key, results);
    return results;
  }

  /** Every end position one occurrence of the particle can reach from {@code pos}. */
  private List<Result<Unit>> unit(Particle particle, int pos) {
    return switch (particle) {
      case Name name ->
          pos < names.size() && names.get(pos).equals(name.name())
              ? List.of(new Result<>(pos + 1, new Element(pos)))
              : List.of();
      case Sequence sequence -> items(sequence.items(), 0, pos);
      case Choice choice -> {
        Map<Integer, Result<Unit>> byEnd = new LinkedHashMap<>();
        for (Particle option : choice.options()) {
          for (Result<Part> result : part(option, pos)) {
            byEnd.putIfAbsent(result.end(), new Result<>(result.end(), new Option(result.value())));
          }
        }
        yield List.copyOf(byEnd.values());
      }
    };
  }

  private List<Result<Unit>> items(List<Particle> items, int index, int pos) {
    if (index == items.size()) {
      return List.of(new Result<>(pos, new Items(List.of())));
    }
    Map<Integer, Result<Unit>> byEnd = new LinkedHashMap<>();
    for (Result<Part> head : part(items.get(index), pos)) {
      for (Result<Unit> rest : items(items, index + 1, head.end())) {
        List<Part> combined = new ArrayList<>();
        combined.add(head.value());
        combined.addAll(((Items) rest.value()).parts());
        byEnd.putIfAbsent(rest.end(), new Result<>(rest.end(), new Items(combined)));
      }
    }
    return List.copyOf(byEnd.values());
  }
}
