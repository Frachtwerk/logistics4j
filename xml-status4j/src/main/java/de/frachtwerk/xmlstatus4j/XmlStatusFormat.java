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

package de.frachtwerk.xmlstatus4j;

import java.util.List;

/**
 * Reads and writes XML status files with the {@linkplain XmlStatusConfig#defaults() defaults}.
 *
 * <p>Reading ignores a {@code DOCTYPE} declaration and removes line breaks inside text values.
 * Writing produces an indented document with an ISO-8859-1 declaration; the caller encodes the
 * lines accordingly. For other settings, e.g. another charset, use {@link XmlStatusCodec}.
 */
public final class XmlStatusFormat {

  public static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>";

  private XmlStatusFormat() {}

  /**
   * @throws XmlStatusException if the lines are not a well-formed status document
   */
  public static FahrDaten read(List<String> lines) {
    return XmlStatusCodec.create().read(lines);
  }

  /**
   * @throws XmlStatusException if the object cannot be serialized
   */
  public static List<String> write(FahrDaten fahrDaten) {
    return XmlStatusCodec.create().write(fahrDaten);
  }
}
