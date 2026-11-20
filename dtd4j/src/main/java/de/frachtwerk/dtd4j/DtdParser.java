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

package de.frachtwerk.dtd4j;

import de.frachtwerk.dtd4j.ContentModel.Any;
import de.frachtwerk.dtd4j.ContentModel.Children;
import de.frachtwerk.dtd4j.ContentModel.Empty;
import de.frachtwerk.dtd4j.ContentModel.Mixed;
import de.frachtwerk.dtd4j.ContentModel.Text;
import de.frachtwerk.dtd4j.Particle.Choice;
import de.frachtwerk.dtd4j.Particle.Name;
import de.frachtwerk.dtd4j.Particle.Sequence;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads the element declarations of a DTD. Attribute lists are skipped; entities, notations and
 * conditional sections are not supported.
 */
final class DtdParser {

  private final String source;
  private final String text;
  private final List<String> errors;
  private final Map<String, ElementDeclaration> elements = new LinkedHashMap<>();
  private int pos;

  private DtdParser(String source, String text, List<String> errors) {
    this.source = source;
    this.text = text;
    this.errors = errors;
  }

  /** The declarations in source order; problems go to {@code errors}. */
  static Map<String, ElementDeclaration> parse(String source, String text, List<String> errors) {
    DtdParser parser = new DtdParser(source, text, errors);
    parser.declarations();
    return parser.elements;
  }

  private void declarations() {
    while (true) {
      skipSpace();
      if (pos >= text.length()) {
        return;
      }
      int start = pos;
      try {
        if (text.startsWith("<!--", pos)) {
          skipPast("-->");
        } else if (text.startsWith("<?", pos)) {
          skipPast("?>");
        } else if (text.startsWith("<!ELEMENT", pos)) {
          element();
        } else if (text.startsWith("<!ATTLIST", pos)) {
          skipDeclaration();
        } else if (text.startsWith("<!ENTITY", pos) || text.startsWith("%", pos)) {
          throw new ParseException("entities are not supported");
        } else if (text.startsWith("<!NOTATION", pos) || text.startsWith("<![", pos)) {
          throw new ParseException("notations and conditional sections are not supported");
        } else {
          throw new ParseException("unexpected '" + snippet() + "'");
        }
      } catch (ParseException e) {
        errors.add(source + ":" + line(start) + ": " + e.getMessage());
        // continue after the declaration
        pos = start;
        skipDeclarationOrEnd();
      }
    }
  }

  private void element() {
    int start = pos;
    pos += "<!ELEMENT".length();
    requireSpace();
    String name = name();
    requireSpace();
    ContentModel content = content();
    skipSpace();
    expect('>');
    if (elements.containsKey(name)) {
      errors.add(source + ":" + line(start) + ": element " + name + " is declared twice");
    } else {
      elements.put(name, new ElementDeclaration(name, content));
    }
  }

  private ContentModel content() {
    if (text.startsWith("EMPTY", pos)) {
      pos += "EMPTY".length();
      return new Empty();
    }
    if (text.startsWith("ANY", pos)) {
      pos += "ANY".length();
      return new Any();
    }
    int open = pos;
    expect('(');
    skipSpace();
    if (text.startsWith("#PCDATA", pos)) {
      return mixed();
    }
    pos = open;
    return new Children(group());
  }

  private ContentModel mixed() {
    pos += "#PCDATA".length();
    Set<String> names = new LinkedHashSet<>();
    skipSpace();
    while (peek() == '|') {
      pos++;
      skipSpace();
      names.add(name());
      skipSpace();
    }
    expect(')');
    if (names.isEmpty()) {
      if (peek() == '*') {
        pos++;
      }
      return new Text();
    }
    expect('*');
    return new Mixed(names);
  }

  /** A sequence or choice in parentheses, with its occurrence. */
  private Particle group() {
    expect('(');
    List<Particle> parts = new ArrayList<>();
    Character separator = null;
    while (true) {
      skipSpace();
      parts.add(particle());
      skipSpace();
      char c = peek();
      if (c == ')') {
        pos++;
        break;
      }
      if (c != ',' && c != '|') {
        throw new ParseException("expected ',', '|' or ')' but found '" + snippet() + "'");
      }
      if (separator != null && separator != c) {
        throw new ParseException("',' and '|' mixed in one group");
      }
      separator = c;
      pos++;
    }
    Occurs occurs = occurs();
    if (separator != null && separator == '|') {
      return new Choice(parts, occurs);
    }
    return new Sequence(parts, occurs);
  }

  private Particle particle() {
    if (peek() == '(') {
      return group();
    }
    return new Name(name(), occurs());
  }

  private Occurs occurs() {
    return switch (peek()) {
      case '?' -> {
        pos++;
        yield Occurs.OPTIONAL;
      }
      case '*' -> {
        pos++;
        yield Occurs.ZERO_OR_MORE;
      }
      case '+' -> {
        pos++;
        yield Occurs.ONE_OR_MORE;
      }
      default -> Occurs.ONE;
    };
  }

  private String name() {
    int start = pos;
    while (pos < text.length() && isNameChar(text.charAt(pos), pos == start)) {
      pos++;
    }
    if (pos == start) {
      throw new ParseException("expected a name but found '" + snippet() + "'");
    }
    return text.substring(start, pos);
  }

  private static boolean isNameChar(char c, boolean first) {
    if (Character.isLetter(c) || c == '_' || c == ':') {
      return true;
    }
    return !first && (Character.isDigit(c) || c == '-' || c == '.');
  }

  // ---- low-level helpers ------------------------------------------------------------------------

  private char peek() {
    return pos < text.length() ? text.charAt(pos) : '\0';
  }

  private void expect(char c) {
    if (peek() != c) {
      throw new ParseException("expected '" + c + "' but found '" + snippet() + "'");
    }
    pos++;
  }

  private void requireSpace() {
    if (!Character.isWhitespace(peek())) {
      throw new ParseException("expected white space but found '" + snippet() + "'");
    }
    skipSpace();
  }

  private void skipSpace() {
    while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
      pos++;
    }
  }

  private void skipPast(String end) {
    int found = text.indexOf(end, pos);
    if (found < 0) {
      throw new ParseException("missing '" + end + "'");
    }
    pos = found + end.length();
  }

  /** Skips a declaration, respecting quoted strings that may contain '>'. */
  private void skipDeclaration() {
    char quote = 0;
    while (pos < text.length()) {
      char c = text.charAt(pos++);
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if (c == '"' || c == '\'') {
        quote = c;
      } else if (c == '>') {
        return;
      }
    }
    throw new ParseException("missing '>'");
  }

  private void skipDeclarationOrEnd() {
    int end = text.indexOf('>', pos);
    pos = end < 0 ? text.length() : end + 1;
  }

  private String snippet() {
    if (pos >= text.length()) {
      return "end of file";
    }
    return text.substring(pos, Math.min(text.length(), pos + 20)).replaceAll("\\s+", " ");
  }

  private int line(int offset) {
    int line = 1;
    for (int i = 0; i < offset && i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        line++;
      }
    }
    return line;
  }

  private static final class ParseException extends RuntimeException {
    ParseException(String message) {
      super(message, null, false, false);
    }
  }
}
