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

import java.util.List;

/**
 * Writes {@link XmlElement}s as text: two spaces per level when indenting, {@code <X/>} for an
 * element without content and {@code <X></X>} for empty text.
 */
final class XmlFormatter {

  private static final String NIL =
      " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"";

  private final boolean indent;
  private final StringBuilder out = new StringBuilder();

  private XmlFormatter(boolean indent) {
    this.indent = indent;
  }

  /** The declaration, if any, and the document as lines. */
  static List<String> format(XmlElement root, XmlLayout layout) {
    XmlFormatter formatter = new XmlFormatter(layout.indent());
    formatter.element(root, 0);
    String body = formatter.out.toString();
    String xml = layout.declaration().map(d -> d + "\n" + body).orElse(body);
    return List.of(xml.split("\\r?\\n"));
  }

  private void element(XmlElement element, int depth) {
    newLine(depth);
    out.append('<').append(element.name());
    if (element.nil()) {
      out.append(NIL);
    }
    if (element.children().isEmpty()) {
      if (element.text() == null) {
        out.append("/>");
      } else {
        out.append('>').append(escape(element.text())).append("</").append(element.name());
        out.append('>');
      }
      return;
    }
    out.append('>');
    for (XmlElement child : element.children()) {
      element(child, depth + 1);
    }
    newLine(depth);
    out.append("</").append(element.name()).append('>');
  }

  private void newLine(int depth) {
    if (indent && !out.isEmpty()) {
      out.append('\n').append("  ".repeat(depth));
    }
  }

  /**
   * Escapes like Woodstox: {@code &}, {@code <}, the {@code >} of {@code ]]>}, CR and the C1
   * controls as references; other C0 controls are not allowed in XML 1.0.
   *
   * @throws Dtd4jException for a character XML 1.0 does not allow
   */
  static String escape(String text) {
    StringBuilder escaped = new StringBuilder(text.length());
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '&') {
        escaped.append("&amp;");
      } else if (c == '<') {
        escaped.append("&lt;");
      } else if (c == '>' && i >= 2 && text.startsWith("]]", i - 2)) {
        escaped.append("&gt;");
      } else if (c == '\r' || (c >= 0x7f && c <= 0x9f)) {
        escaped.append("&#x").append(Integer.toHexString(c)).append(';');
      } else if ((c < 0x20 && c != '\t' && c != '\n') || c == 0xfffe || c == 0xffff) {
        throw new Dtd4jException(
            String.format("character U+%04X is not allowed in XML: %s", (int) c, text));
      } else {
        escaped.append(c);
      }
    }
    return escaped.toString();
  }
}
