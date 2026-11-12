package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.schema.CodeList;
import java.util.Map;

/** What a {@link FieldType} may know about the field it creates a codec for. */
public record FieldContext(String fieldName, int length, Map<String, CodeList> codeLists) {}
