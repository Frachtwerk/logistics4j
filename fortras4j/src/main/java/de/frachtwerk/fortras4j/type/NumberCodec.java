/*
 * Copyright (C) 2026 Frachtwerk GmbH, Leopoldstraße 7C, 76133 Karlsruhe.
 *
 * This file is part of logistics4j.
 *
 * logistics4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * logistics4j is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with logistics4j. If not, see <http://www.gnu.org/licenses/>.
 */

package de.frachtwerk.fortras4j.type;

import de.frachtwerk.fortras4j.ProblemCode;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Numeric fields. Type {@code integer} yields {@link Long}, type {@code decimal} yields {@link
 * BigDecimal}.
 *
 * <p>Decimals use implied decimal places by default ({@code scale: 3} reads "000012500" as 12.5),
 * as FORTRAS prescribes. With {@code point: explicit} the value carries a decimal point and is
 * written with as many decimal places as fit (geo coordinates).
 */
final class NumberCodec implements FieldCodec {

  enum Point {
    IMPLIED,
    EXPLICIT
  }

  private static final Pattern DIGITS = Pattern.compile("[+-]?\\d+");

  private final boolean decimal;
  private final int scale;
  private final Point point;
  private final Layout layout;

  private NumberCodec(boolean decimal, int scale, Point point, Layout layout) {
    this.decimal = decimal;
    this.scale = scale;
    this.point = point;
    this.layout = layout;
  }

  static FieldType integerType() {
    return new FieldType() {
      @Override
      public String name() {
        return "integer";
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        Layout numberLayout =
            Layout.from(
                options, Layout.Align.RIGHT, '0', Layout.NullAs.ZEROS, Layout.Overflow.ERROR);
        checkOverflow(numberLayout);
        return new NumberCodec(false, 0, Point.IMPLIED, numberLayout);
      }
    };
  }

  static FieldType decimalType() {
    return new FieldType() {
      @Override
      public String name() {
        return "decimal";
      }

      @Override
      public FieldCodec create(FieldOptions options, FieldContext context) {
        Point decimalPoint = options.enumValue("point", Point.class, Point.IMPLIED);
        int decimals = options.integer("scale", 0);
        if (decimals < 0 || (decimalPoint == Point.IMPLIED && decimals > context.length())) {
          throw new IllegalArgumentException("scale must be between 0 and the field length");
        }
        Layout numberLayout =
            decimalPoint == Point.IMPLIED
                ? Layout.from(
                    options, Layout.Align.RIGHT, '0', Layout.NullAs.ZEROS, Layout.Overflow.ERROR)
                : Layout.from(
                    options, Layout.Align.LEFT, ' ', Layout.NullAs.BLANKS, Layout.Overflow.ERROR);
        checkOverflow(numberLayout);
        return new NumberCodec(true, decimals, decimalPoint, numberLayout);
      }
    };
  }

  private static void checkOverflow(Layout layout) {
    if (layout.overflow() == Layout.Overflow.TRUNCATE) {
      throw new IllegalArgumentException("overflow 'truncate' is not allowed for numbers");
    }
  }

  @Override
  public Object parse(String slice) {
    String text = slice.strip();
    if (text.isEmpty()) {
      return null;
    }
    try {
      if (point == Point.EXPLICIT) {
        return new BigDecimal(text);
      }
      if (!DIGITS.matcher(text).matches()) {
        throw new NumberFormatException(text);
      }
      if (!decimal) {
        return Long.parseLong(text.startsWith("+") ? text.substring(1) : text);
      }
      return new BigDecimal(new BigInteger(text), scale);
    } catch (NumberFormatException | ArithmeticException e) {
      throw new FieldValueException(ProblemCode.INVALID_VALUE, "'" + text + "' is not a number");
    }
  }

  @Override
  public String format(Object value, int length) {
    if (value == null) {
      return layout.empty(length);
    }
    BigDecimal number = toBigDecimal(value);
    if (point == Point.EXPLICIT) {
      return formatExplicit(number, length);
    }
    BigInteger unscaled =
        decimal ? number.setScale(scale, RoundingMode.HALF_UP).unscaledValue() : integer(number);
    String digits = unscaled.abs().toString();
    String sign = unscaled.signum() < 0 ? "-" : "";
    if (sign.length() + digits.length() > length) {
      if (layout.overflow() == Layout.Overflow.MAX) {
        return sign + "9".repeat(length - sign.length());
      }
      throw new FieldValueException(
          ProblemCode.VALUE_TOO_LONG,
          number.toPlainString() + " does not fit into " + length + " digits");
    }
    if (layout.pad() == '0' && layout.align() == Layout.Align.RIGHT) {
      return sign + "0".repeat(length - sign.length() - digits.length()) + digits;
    }
    return layout.fit(sign + digits, length);
  }

  private String formatExplicit(BigDecimal number, int length) {
    BigDecimal candidate = number.stripTrailingZeros();
    if (candidate.scale() < 0) {
      candidate = candidate.setScale(0);
    }
    // FORTRAS: "Anzahl Nachkommastellen bis zu maximaler Feldlänge"
    while (candidate.toPlainString().length() > length && candidate.scale() > 0) {
      candidate = candidate.setScale(candidate.scale() - 1, RoundingMode.HALF_UP);
    }
    String text = candidate.toPlainString();
    if (text.length() > length && layout.overflow() == Layout.Overflow.MAX) {
      return (number.signum() < 0 ? "-" : "") + "9".repeat(length - (number.signum() < 0 ? 1 : 0));
    }
    return layout.fit(text, length);
  }

  @Override
  public Object coerce(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof String text && text.isBlank()) {
      return null;
    }
    BigDecimal number = toBigDecimal(value);
    if (!decimal) {
      return integer(number).longValueExact();
    }
    return number;
  }

  private static BigInteger integer(BigDecimal number) {
    try {
      return number.toBigIntegerExact();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException(number.toPlainString() + " is not an integer");
    }
  }

  private static BigDecimal toBigDecimal(Object value) {
    return switch (value) {
      case BigDecimal decimal -> decimal;
      case BigInteger integer -> new BigDecimal(integer);
      case Long l -> BigDecimal.valueOf(l);
      case Integer i -> BigDecimal.valueOf(i);
      case Short s -> BigDecimal.valueOf(s);
      case Byte b -> BigDecimal.valueOf(b);
      case Number n -> new BigDecimal(n.toString());
      case String s -> {
        try {
          yield new BigDecimal(s.strip());
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("'" + s + "' is not a number");
        }
      }
      default ->
          throw new IllegalArgumentException(
              "cannot convert " + value.getClass().getSimpleName() + " to a number");
    };
  }
}
