package de.frachtwerk.fortras4j.type;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Type-specific settings of a field as written in the layout file. Tracks which options were read,
 * so the loader can reject misspelled ones.
 */
public final class FieldOptions {

  private final Map<String, Object> values;
  private final Set<String> used = new HashSet<>();

  public FieldOptions(Map<String, Object> values) {
    this.values = new LinkedHashMap<>(values);
  }

  public boolean has(String key) {
    return values.containsKey(key);
  }

  public String string(String key, String defaultValue) {
    Object value = take(key);
    return value == null ? defaultValue : value.toString();
  }

  public int integer(String key, int defaultValue) {
    Object value = take(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(value.toString().trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("option '" + key + "' must be a number: " + value);
    }
  }

  public boolean bool(String key, boolean defaultValue) {
    Object value = take(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof Boolean b) {
      return b;
    }
    return switch (value.toString().trim().toLowerCase(Locale.ROOT)) {
      case "true", "yes" -> true;
      case "false", "no" -> false;
      default ->
          throw new IllegalArgumentException(
              "option '" + key + "' must be true or false: " + value);
    };
  }

  public char character(String key, char defaultValue) {
    Object value = take(key);
    if (value == null) {
      return defaultValue;
    }
    String text = value.toString();
    if (text.length() != 1) {
      throw new IllegalArgumentException(
          "option '" + key + "' must be a single character: '" + text + "'");
    }
    return text.charAt(0);
  }

  /** Accepts a single value or a list; numbers are kept as written ({@code 1} becomes "1"). */
  public List<String> strings(String key, List<String> defaultValue) {
    Object value = take(key);
    if (value == null) {
      return defaultValue;
    }
    if (value instanceof List<?> list) {
      return list.stream().map(FieldOptions::text).toList();
    }
    return List.of(text(value));
  }

  public <E extends Enum<E>> E enumValue(String key, Class<E> type, E defaultValue) {
    Object value = take(key);
    if (value == null) {
      return defaultValue;
    }
    String text = value.toString().trim().toUpperCase(Locale.ROOT).replace('-', '_');
    try {
      return Enum.valueOf(type, text);
    } catch (IllegalArgumentException e) {
      List<String> allowed =
          java.util.Arrays.stream(type.getEnumConstants())
              .map(c -> c.name().toLowerCase(Locale.ROOT))
              .toList();
      throw new IllegalArgumentException(
          "option '" + key + "' must be one of " + allowed + ": " + value);
    }
  }

  /** Options present in the layout but never read by the type. */
  public Set<String> unused() {
    Set<String> unused = new HashSet<>(values.keySet());
    unused.removeAll(used);
    return unused;
  }

  private Object take(String key) {
    used.add(key);
    return values.get(key);
  }

  private static String text(Object value) {
    if (value instanceof BigDecimal decimal) {
      return decimal.toPlainString();
    }
    return String.valueOf(value);
  }
}
