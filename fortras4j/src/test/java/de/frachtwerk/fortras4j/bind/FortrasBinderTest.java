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

package de.frachtwerk.fortras4j.bind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import de.frachtwerk.fortras4j.Fortras4j;
import de.frachtwerk.fortras4j.FortrasDocument;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FortrasBinderTest {

  record File(Header header, List<Order> orders, List<Message> message, Trailer trailer) {}

  record Header(LocalDate created) {}

  record Order(OrderLine order, List<Item> items) {}

  record OrderLine(int orderNo, String colour, BigDecimal weight, Boolean express) {}

  record Item(int orderNo, String size, List<Note> notes) {}

  record Note(String key, String text) {}

  record Message(String text) {}

  record Trailer(int count) {}

  private static final Fortras4j FORTRAS =
      Fortras4j.of(FortrasSchemas.builder().addClasspath("layouts/demo.yaml").build());

  private static final List<String> LINES =
      List.of(
          "HDR00112112026",
          "ORD001RED001250Y",
          "ITM001SN1abcd",
          "MSGhello",
          "ORD002GRN000100N",
          "END0005");

  @Test
  void bindsGroupsChoicesAndBlocks() {
    File file = FORTRAS.read(LINES, File.class);

    assertThat(file.header().created()).isEqualTo(LocalDate.of(2026, 11, 12));
    assertThat(file.orders()).extracting(o -> o.order().orderNo()).containsExactly(1, 2);
    assertThat(file.orders().getFirst().items().getFirst().notes())
        .containsExactly(new Note("N1", "abcd"));
    assertThat(file.message()).containsExactly(new Message("hello"));
    assertThat(file.trailer().count()).isEqualTo(5);
  }

  @Test
  void writesObjectsBackInStructureOrder() {
    File file = FORTRAS.read(LINES, File.class);

    List<String> written = FORTRAS.write(file, "demo");

    // options of a choice are written in declaration order: all orders, then all messages
    assertThat(written)
        .extracting(l -> l.substring(0, 3))
        .containsExactly("HDR", "ORD", "ITM", "ORD", "MSG", "END");
    FortrasDocument reread = FORTRAS.read(written);
    assertThat(reread.problems()).isEmpty();
    assertThat(FORTRAS.binder().toObject(reread, File.class)).isEqualTo(file);
  }

  @Test
  void exposesGenericMaps() {
    Map<String, Object> map = FORTRAS.binder().toMap(FORTRAS.read(LINES).root());

    assertThat(map).containsOnlyKeys("header", "orders", "message", "trailer");
    assertThat(map.get("orders")).asInstanceOf(LIST).hasSize(2);
  }
}
