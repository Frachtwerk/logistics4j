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

import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.format.DateTimeFormatter;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Root element {@code FAR_DATA} of an XML status file: trips ({@code FAHRT}) with shipments ({@code
 * SDG}), their status and the barcodes of their packages. Read and written with {@link
 * XmlStatusFormat}.
 */
@JsonRootName("FAR_DATA")
public class FahrDaten {
  public static final DateTimeFormatter BASIC_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  public static final DateTimeFormatter BASIC_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HHmm[ss]");
  public static final DateTimeFormatter BASIC_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  public static final DateTimeFormatter BASIC_DATE_TIME_FORMATTER_WITHOUT_SECONDS =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "FAHRT")
  private List<Fahrt> fahrten;

  public List<Fahrt> getFahrten() {
    return fahrten;
  }

  public void setFahrten(List<Fahrt> fahrten) {
    this.fahrten = fahrten;
  }
}
