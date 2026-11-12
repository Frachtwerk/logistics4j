package de.frachtwerk.fortras4j.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Layout of one record type ("Satzart"), identified by the prefix its lines start with.
 *
 * @param length length of a newly written line; at least the end of the last field
 */
public record RecordDefinition(
    String id, String prefix, String description, List<FieldElement> elements, int length) {

  public RecordDefinition {
    elements = List.copyOf(elements);
  }

  public FieldElement element(String name) {
    for (FieldElement element : elements) {
      if (element.name().equals(name)) {
        return element;
      }
    }
    return null;
  }

  public boolean hasField(String name) {
    return element(name) instanceof FieldDefinition;
  }

  public Map<String, FieldElement> elementsByName() {
    Map<String, FieldElement> result = new LinkedHashMap<>();
    elements.forEach(element -> result.put(element.name(), element));
    return result;
  }
}
