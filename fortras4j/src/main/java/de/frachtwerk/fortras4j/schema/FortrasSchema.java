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

package de.frachtwerk.fortras4j.schema;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** A loaded, validated layout: record layouts, code lists and file structure of one format. */
public final class FortrasSchema {

  private final String id;
  private final String format;
  private final String release;
  private final String description;
  private final Pattern detect;
  private final int maxLineLength;
  private final boolean trimTrailingSpaces;
  private final Pattern passthrough;
  private final Map<String, RecordDefinition> records;
  private final List<RecordDefinition> recordsByPrefixLength;
  private final Map<String, CodeList> codeLists;
  private final StructureNode.GroupNode root;
  private final Map<String, ValueRule> rules;

  FortrasSchema(
      String id,
      String format,
      String release,
      String description,
      Pattern detect,
      int maxLineLength,
      boolean trimTrailingSpaces,
      Pattern passthrough,
      Map<String, RecordDefinition> records,
      Map<String, CodeList> codeLists,
      StructureNode.GroupNode root,
      Map<String, ValueRule> rules) {
    this.id = id;
    this.format = format;
    this.release = release;
    this.description = description;
    this.detect = detect;
    this.maxLineLength = maxLineLength;
    this.trimTrailingSpaces = trimTrailingSpaces;
    this.passthrough = passthrough;
    this.records = Map.copyOf(new LinkedHashMap<>(records));
    this.recordsByPrefixLength =
        records.values().stream()
            .sorted(Comparator.comparingInt((RecordDefinition r) -> r.prefix().length()).reversed())
            .toList();
    this.codeLists = Map.copyOf(codeLists);
    this.root = root;
    this.rules = Collections.unmodifiableMap(new LinkedHashMap<>(rules));
  }

  public String id() {
    return id;
  }

  public String format() {
    return format;
  }

  public String release() {
    return release;
  }

  public String description() {
    return description;
  }

  public int maxLineLength() {
    return maxLineLength;
  }

  /** Whether newly written lines end after the last non-blank character. */
  public boolean trimTrailingSpaces() {
    return trimTrailingSpaces;
  }

  /** True if this layout claims a file, judged by its first line. */
  public boolean matches(String firstLine) {
    return detect != null && detect.matcher(firstLine).find();
  }

  /**
   * Lines matching this pattern are kept verbatim without a problem, e.g. bilateral X99 records.
   */
  public boolean isPassthrough(String line) {
    return passthrough != null && passthrough.matcher(line).find();
  }

  public RecordDefinition record(String recordId) {
    return records.get(recordId);
  }

  public Map<String, RecordDefinition> records() {
    return records;
  }

  /** The record type of a line: the definition with the longest matching prefix. */
  public RecordDefinition recordFor(String line) {
    for (RecordDefinition definition : recordsByPrefixLength) {
      if (line.startsWith(definition.prefix())) {
        return definition;
      }
    }
    return null;
  }

  public Map<String, CodeList> codeLists() {
    return codeLists;
  }

  public CodeList codeList(String name) {
    return codeLists.get(name);
  }

  /** All value rules, by name, in layout order. */
  public Map<String, ValueRule> rules() {
    return rules;
  }

  public ValueRule rule(String name) {
    return rules.get(name);
  }

  /** The value rules of one record type, in layout order. */
  public List<ValueRule> rules(String recordId) {
    return rules.values().stream().filter(r -> r.recordId().equals(recordId)).toList();
  }

  /** The whole file as a group that occurs exactly once. */
  public StructureNode.GroupNode root() {
    return root;
  }

  @Override
  public String toString() {
    return "FortrasSchema[" + id + "]";
  }
}
