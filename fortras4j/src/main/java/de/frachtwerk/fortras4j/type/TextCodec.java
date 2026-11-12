package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import de.frachtwerk.fortras4j.Severity;
import de.frachtwerk.fortras4j.schema.CodeList;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Alphanumeric fields. Types {@code string}, {@code code} (validated against a code list) and
 * {@code currency} (ISO 4217).
 *
 * <p>Options: {@code trim} ({@code both}, {@code right}, {@code none}); {@code maxLength} (allowed
 * length below the field length, e.g. 20 for bordero numbers); {@code valuePrefix} (written in
 * front of the value and removed when reading, e.g. a partner prefix); {@code codelist} and {@code
 * strict} for codes.
 */
final class TextCodec implements FieldCodec {

  enum Trim {
    BOTH,
    RIGHT,
    NONE
  }

  enum Kind {
    STRING,
    CODE,
    CURRENCY
  }

  private static final Pattern LINE_BREAK = Pattern.compile("\\R");

  private final Kind kind;
  private final Layout layout;
  private final Trim trim;
  private final int maxLength;
  private final String valuePrefix;
  private final CodeList codeList;
  private final boolean strict;

  private TextCodec(
      Kind kind,
      Layout layout,
      Trim trim,
      int maxLength,
      String valuePrefix,
      CodeList codeList,
      boolean strict) {
    this.kind = kind;
    this.layout = layout;
    this.trim = trim;
    this.maxLength = maxLength;
    this.valuePrefix = valuePrefix;
    this.codeList = codeList;
    this.strict = strict;
  }

  static FieldType type(String name, Kind kind) {
    return new FieldType() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        Layout layout =
            Layout.from(
                options,
                Layout.Align.LEFT,
                ' ',
                Layout.NullAs.BLANKS,
                kind == Kind.STRING ? Layout.Overflow.TRUNCATE : Layout.Overflow.ERROR);
        if (layout.overflow() == Layout.Overflow.MAX) {
          throw new IllegalArgumentException("overflow 'max' is only allowed for numbers");
        }
        Trim trim = options.enumValue("trim", Trim.class, Trim.BOTH);
        int maxLength = options.integer("maxLength", context.length());
        if (maxLength < 1 || maxLength > context.length()) {
          throw new IllegalArgumentException(
              "maxLength must be between 1 and the field length " + context.length());
        }
        String valuePrefix = options.string("valuePrefix", null);
        CodeList codeList = null;
        boolean strict = false;
        if (kind == Kind.CODE) {
          String listName = options.string("codelist", null);
          if (listName == null) {
            throw new IllegalArgumentException("type 'code' requires option 'codelist'");
          }
          codeList = context.codeLists().get(listName);
          if (codeList == null) {
            throw new IllegalArgumentException("unknown codelist '" + listName + "'");
          }
          strict = options.bool("strict", codeList.strict());
        }
        return new TextCodec(kind, layout, trim, maxLength, valuePrefix, codeList, strict);
      }
    };
  }

  @Override
  public Object parse(String slice) {
    String text =
        switch (trim) {
          case BOTH -> slice.strip();
          case RIGHT -> slice.stripTrailing();
          case NONE -> slice;
        };
    if (text.isBlank()) {
      return null;
    }
    if (valuePrefix != null && text.startsWith(valuePrefix)) {
      text = text.substring(valuePrefix.length());
    }
    validate(text, false);
    return text;
  }

  @Override
  public String format(Object value, int length) {
    if (value == null) {
      return layout.empty(length);
    }
    String text = LINE_BREAK.matcher(value.toString()).replaceAll(" ");
    validate(text, true);
    if (valuePrefix != null) {
      text = valuePrefix + text;
    }
    return layout.fit(text, length);
  }

  @Override
  public Object coerce(Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString();
    return text.isEmpty() ? null : text;
  }

  /** When writing, codes missing from a lenient code list are written without complaint. */
  private void validate(String text, boolean writing) {
    int length = text.length() + (valuePrefix == null ? 0 : valuePrefix.length());
    if (length > maxLength) {
      throw new FieldValueException(
          ProblemCode.VALUE_TOO_LONG,
          Severity.ERROR,
          "value '" + text + "' exceeds the allowed length of " + maxLength,
          text);
    }
    switch (kind) {
      case CODE -> {
        if (!codeList.contains(text) && !(writing && !strict)) {
          throw new FieldValueException(
              ProblemCode.UNKNOWN_CODE,
              strict ? Severity.ERROR : Severity.WARNING,
              "code '" + text + "' is not in codelist '" + codeList.name() + "'",
              text);
        }
      }
      case CURRENCY -> {
        try {
          Currency.getInstance(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
          throw new FieldValueException(
              ProblemCode.INVALID_VALUE,
              Severity.ERROR,
              "'" + text + "' is not an ISO 4217 currency",
              text);
        }
      }
      case STRING -> {
        // no further checks
      }
    }
  }
}
