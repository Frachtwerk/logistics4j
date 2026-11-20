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

/** Classes of the application for {@code tours.dtd}; the layout {@code tours.yaml} binds them. */
final class Tours {

  private Tours() {}

  record Root(List<Tour> tours) {}

  record Tour(List<Leg> legs) {}

  record Leg(LegHead head, List<Shipment> shipments) {}

  record LegHead(String number, String driver) {}

  record Shipment(Head head, List<Item> items) {}

  record Head(String id, Sender sender, Status status, String note) {}

  record Sender(String name, String city) {}

  record Status(String code, String text) {}

  record Item(List<Position> positions) {}

  record Position(String count, String label, List<Codes> codes) {}

  record Codes(List<Entry> entries) {}

  record Entry(String kind, String value, String state) {}

  static XmlLayout layout() {
    return XmlLayout.loadClasspath("dtd/tours.yaml", Tours.class.getClassLoader());
  }

  static String shipment(String id) {
    return "<SHIPMENT><HEAD><ID>"
        + id
        + "</ID><STATUS><CODE>1</CODE></STATUS></HEAD>"
        + "<ITEM><COUNT>2</COUNT><LABEL>a</LABEL>"
        + "<CODES><KIND>N</KIND><VALUE>v1</VALUE><KIND>N</KIND><VALUE>v2</VALUE></CODES>"
        + "<CODES><KIND>N</KIND><VALUE>v3</VALUE></CODES>"
        + "<LABEL>b</LABEL></ITEM></SHIPMENT>";
  }

  /** {@code (LEG_HEAD?, SHIPMENT+)+} twice, {@code ITEM (...)+} and {@code CODES (...)*}. */
  static final String REPEATED =
      "<TOURS><TOUR>"
          + "<LEG_HEAD><LEG_NO>T1</LEG_NO></LEG_HEAD>"
          + shipment("S1")
          + shipment("S2")
          + "<LEG_HEAD><LEG_NO>T2</LEG_NO></LEG_HEAD>"
          + shipment("S3")
          + "</TOUR></TOURS>";
}
