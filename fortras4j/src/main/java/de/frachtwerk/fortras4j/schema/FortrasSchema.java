package de.frachtwerk.fortras4j.schema;

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
      StructureNode.GroupNode root) {
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

  /** The whole file as a group that occurs exactly once. */
  public StructureNode.GroupNode root() {
    return root;
  }

  @Override
  public String toString() {
    return "FortrasSchema[" + id + "]";
  }
}
