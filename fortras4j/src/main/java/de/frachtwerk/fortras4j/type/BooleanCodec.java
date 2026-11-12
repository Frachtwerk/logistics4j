package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import java.util.List;
import java.util.Locale;

/**
 * Type {@code boolean}. Options {@code true} and {@code false} list the accepted codes; the first
 * one is written. Defaults: {@code Y} / {@code N}.
 */
final class BooleanCodec implements FieldCodec {

  private final List<String> trueCodes;
  private final List<String> falseCodes;
  private final Layout layout;

  private BooleanCodec(List<String> trueCodes, List<String> falseCodes, Layout layout) {
    this.trueCodes = trueCodes;
    this.falseCodes = falseCodes;
    this.layout = layout;
  }

  static FieldType type() {
    return new FieldType() {
      @Override
      public String name() {
        return "boolean";
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        List<String> trueCodes = options.strings("true", List.of("Y"));
        List<String> falseCodes = options.strings("false", List.of("N"));
        if (trueCodes.isEmpty() || falseCodes.isEmpty()) {
          throw new IllegalArgumentException("options 'true' and 'false' must not be empty");
        }
        Layout layout =
            Layout.from(
                options, Layout.Align.LEFT, ' ', Layout.NullAs.BLANKS, Layout.Overflow.ERROR);
        return new BooleanCodec(trueCodes, falseCodes, layout);
      }
    };
  }

  @Override
  public Object parse(String slice) {
    String text = slice.strip();
    if (text.isEmpty()) {
      return null;
    }
    if (trueCodes.contains(text)) {
      return Boolean.TRUE;
    }
    if (falseCodes.contains(text)) {
      return Boolean.FALSE;
    }
    throw new FieldValueException(
        ProblemCode.INVALID_VALUE, "'" + text + "' is neither " + trueCodes + " nor " + falseCodes);
  }

  @Override
  public String format(Object value, int length) {
    if (value == null) {
      return layout.empty(length);
    }
    boolean b = (Boolean) coerce(value);
    return layout.fit(b ? trueCodes.getFirst() : falseCodes.getFirst(), length);
  }

  @Override
  public Object coerce(Object value) {
    return switch (value) {
      case null -> null;
      case Boolean b -> b;
      case String s when s.isBlank() -> null;
      case String s when trueCodes.contains(s.strip()) -> Boolean.TRUE;
      case String s when falseCodes.contains(s.strip()) -> Boolean.FALSE;
      case String s when s.strip().toLowerCase(Locale.ROOT).equals("true") -> Boolean.TRUE;
      case String s when s.strip().toLowerCase(Locale.ROOT).equals("false") -> Boolean.FALSE;
      default -> throw new IllegalArgumentException("cannot convert '" + value + "' to a boolean");
    };
  }
}
