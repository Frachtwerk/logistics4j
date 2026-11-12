package de.frachtwerk.fortras4j.type;

/**
 * A named field type usable in layout files ({@code type: date}). Custom types can be registered
 * via {@link de.frachtwerk.fortras4j.schema.FortrasSchemas.Builder#type(FieldType)}.
 */
public interface FieldType {

  String name();

  /**
   * Creates the codec for one field. Called once while loading the layout, so invalid options are
   * reported at startup and not while parsing.
   *
   * @throws IllegalArgumentException if the options are invalid
   */
  FieldCodec create(FieldOptions options, FieldContext context);
}
