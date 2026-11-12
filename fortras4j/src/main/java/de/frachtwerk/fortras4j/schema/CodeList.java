package de.frachtwerk.fortras4j.schema;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Allowed codes of a field together with their descriptions. A strict code list turns unknown codes
 * into errors, a lenient one into warnings; the value is retained either way.
 */
public record CodeList(String name, Map<String, String> codes, boolean strict) {

  public CodeList {
    // descriptions are optional, so null values must be allowed
    codes = Collections.unmodifiableMap(new LinkedHashMap<>(codes));
  }

  public boolean contains(String code) {
    return codes.containsKey(code);
  }

  public String description(String code) {
    return codes.get(code);
  }
}
