package de.frachtwerk.fortras4j.schema;

import de.frachtwerk.fortras4j.type.FieldCodec;

/**
 * A fixed-width field. A {@code required} field without a value is an error when reading and
 * writing.
 *
 * <p>Positions are 1-based and inclusive, exactly as printed in the FORTRAS record descriptions.
 * Inside a {@link RepeatDefinition} they are relative to the block.
 */
public record FieldDefinition(
    String name,
    int start,
    int end,
    String type,
    FieldCodec codec,
    Object defaultValue,
    boolean required,
    String description)
    implements FieldElement {

  public int length() {
    return end - start + 1;
  }
}
