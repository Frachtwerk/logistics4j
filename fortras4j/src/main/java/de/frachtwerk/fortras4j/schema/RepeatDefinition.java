package de.frachtwerk.fortras4j.schema;

import java.util.List;

/**
 * A block of fields that repeats {@code count} times, every {@code stride} characters, starting at
 * {@code start}; e.g. the six text keys of H00. Its value is a list of maps.
 *
 * <p>With {@code skipEmpty} blocks without any value are left out of the list when reading. If
 * {@code presentIf} names a field, a block counts as present only if that field has a value; useful
 * when unused blocks still carry zeros in numeric fields.
 */
public record RepeatDefinition(
    String name,
    int start,
    int count,
    int stride,
    List<FieldDefinition> fields,
    boolean skipEmpty,
    String presentIf,
    String description)
    implements FieldElement {

  public RepeatDefinition {
    fields = List.copyOf(fields);
  }

  @Override
  public int end() {
    return start + count * stride - 1;
  }

  /** Absolute start position of {@code field} in block {@code index} (0-based). */
  public int absoluteStart(int index, FieldDefinition field) {
    return start + index * stride + field.start() - 1;
  }

  public FieldDefinition field(String fieldName) {
    return fields.stream().filter(f -> f.name().equals(fieldName)).findFirst().orElse(null);
  }
}
