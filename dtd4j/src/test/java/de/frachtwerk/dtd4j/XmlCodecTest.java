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

import static de.frachtwerk.dtd4j.Tours.REPEATED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import de.frachtwerk.dtd4j.Tours.Entry;
import de.frachtwerk.dtd4j.Tours.Head;
import de.frachtwerk.dtd4j.Tours.Item;
import de.frachtwerk.dtd4j.Tours.Leg;
import de.frachtwerk.dtd4j.Tours.Position;
import de.frachtwerk.dtd4j.Tours.Root;
import de.frachtwerk.dtd4j.Tours.Shipment;
import de.frachtwerk.dtd4j.Tours.Status;
import de.frachtwerk.dtd4j.Tours.Tour;
import de.frachtwerk.dtd4j.XmlLayout.Doctype;
import de.frachtwerk.dtd4j.XmlLayout.LineBreaks;
import de.frachtwerk.dtd4j.XmlLayout.NullObjects;
import de.frachtwerk.dtd4j.XmlLayout.Nulls;
import de.frachtwerk.dtd4j.XmlLayout.Order;
import de.frachtwerk.dtd4j.XmlLayout.Validation;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.Test;

class XmlCodecTest {

  private static final XmlLayout LAYOUT = Tours.layout();
  private static final XmlCodec CODEC = XmlCodec.of(LAYOUT);

  @Test
  void readsTheDocumentInItsOrder() {
    XmlDocument document = CODEC.read(List.of(REPEATED));

    assertThat(document.problems()).isEmpty();
    XmlElement tour = document.root().child("TOUR").orElseThrow();
    assertThat(tour.children())
        .extracting(XmlElement::name)
        .containsExactly("LEG_HEAD", "SHIPMENT", "SHIPMENT", "LEG_HEAD", "SHIPMENT");
  }

  @Test
  void bindsTheRepeatedGroupsToObjects() {
    Root root = CODEC.read(List.of(REPEATED), Root.class);

    List<Leg> legs = root.tours().getFirst().legs();
    assertThat(legs).extracting(leg -> leg.head().number()).containsExactly("T1", "T2");
    assertThat(legs).extracting(leg -> leg.shipments().size()).containsExactly(2, 1);
    Item item = legs.getFirst().shipments().getFirst().items().getFirst();
    assertThat(item.positions()).extracting(Position::label).containsExactly("a", "b");
    assertThat(item.positions().getFirst().count()).isEqualTo("2");
    assertThat(item.positions().getFirst().codes())
        .extracting(codes -> codes.entries().stream().map(Entry::value).toList())
        .containsExactly(List.of("v1", "v2"), List.of("v3"));
    assertThat(item.positions().getLast().codes()).isNull();
  }

  @Test
  void writesObjectsInTheOrderOfTheDtd() {
    Root root = CODEC.read(List.of(REPEATED), Root.class);

    List<String> written = codec(b -> b.nulls(Nulls.OMIT)).write(root);

    assertThat(written.getFirst()).isEqualTo("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>");
    assertThat(String.join("", written).replaceAll(">\\s+<", "><"))
        .isEqualTo("<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>" + REPEATED);
    assertThat(CODEC.toDocument(root).problems()).isEmpty();
  }

  @Test
  void writesAlphabeticallyIfConfigured() {
    Head head = new Head("1", null, new Status(null, "t"), "n");
    Root root = root(head);

    List<String> dtd = codec(b -> b.nulls(Nulls.OMIT)).write(root);
    List<String> alphabetical =
        codec(b -> b.nulls(Nulls.OMIT).order(Order.ALPHABETICAL)).write(root);

    assertThat(dtd).containsSubsequence("      <HEAD>", "        <ID>1</ID>", "        <STATUS>");
    assertThat(alphabetical)
        .containsSubsequence("      <HEAD>", "        <ID>1</ID>", "        <NOTE>n</NOTE>");
    // the group occurrences keep their place, their content is sorted
    assertThat(alphabetical).containsSubsequence("      <ITEM>", "        <LABEL>x</LABEL>");
  }

  @Test
  void writesNullsAsConfigured() {
    Root root = root(new Head("1", null, new Status(null, null), null));

    String empty = String.join("\n", CODEC.write(root));
    String nil = String.join("\n", codec(b -> b.nulls(Nulls.NIL)).write(root));
    String omitted = String.join("\n", codec(b -> b.nulls(Nulls.OMIT)).write(root));
    String objects =
        String.join(
            "\n",
            codec(b -> b.nullObjects(NullObjects.NULLS).validation(Validation.OFF)).write(root));

    // tours.yaml leaves out null containers such as SENDER; null values are empty elements
    assertThat(empty).contains("<CODE/>", "<NOTE/>").doesNotContain("SENDER", "xsi");
    assertThat(nil).contains("<CODE xmlns:xsi=", "xsi:nil=\"true\"").doesNotContain("SENDER");
    assertThat(omitted).doesNotContain("CODE", "NOTE", "SENDER");
    assertThat(objects).contains("<SENDER/>");
  }

  @Test
  void reportsDeviationsAndKeepsTheData() {
    String invalid =
        "<TOURS><TOUR><SHIPMENT><HEAD><STATUS/><ID>1</ID></HEAD></SHIPMENT></TOUR></TOURS>";

    XmlDocument document = CODEC.read(List.of(invalid));

    assertThat(document.problems())
        .extracting(Problem::severity, Problem::code, Problem::path)
        .containsExactly(
            tuple(Severity.WARNING, ProblemCode.INVALID_CONTENT, "TOURS/TOUR[1]/SHIPMENT[1]"),
            tuple(
                Severity.WARNING,
                ProblemCode.INVALID_CONTENT,
                "TOURS/TOUR[1]/SHIPMENT[1]/HEAD[1]"));
    Root root = CODEC.bind(document, Root.class);
    Head head = root.tours().getFirst().legs().getFirst().shipments().getFirst().head();
    assertThat(head.id()).isEqualTo("1");
    assertThatThrownBy(
            () -> codec(b -> b.validation(Validation.ERROR)).read(List.of(invalid), Root.class))
        .isInstanceOf(Dtd4jException.class)
        .hasMessageContaining("INVALID_CONTENT");
    assertThat(codec(b -> b.validation(Validation.OFF)).read(List.of(invalid)).problems())
        .isEmpty();
  }

  @Test
  void keepsTheGroupsOfContentThatDoesNotMatch() {
    // the first position lacks its mandatory LABEL, the TOUR starts with a SHIPMENT
    String incomplete =
        "<TOURS><TOUR>"
            + "<SHIPMENT><HEAD><ID>1</ID><STATUS/></HEAD><ITEM>"
            + "<COUNT>1</COUNT><CODES><KIND>N</KIND><VALUE>v1</VALUE></CODES>"
            + "<COUNT>2</COUNT><LABEL>b</LABEL><CODES><KIND>N</KIND><VALUE>v2</VALUE></CODES>"
            + "</ITEM></SHIPMENT>"
            + "<LEG_HEAD><LEG_NO>T2</LEG_NO></LEG_HEAD><LEG_HEAD><LEG_NO>T3</LEG_NO></LEG_HEAD>"
            + "</TOUR></TOURS>";

    XmlDocument document = CODEC.read(List.of(incomplete));
    Root root = CODEC.bind(document, Root.class);

    assertThat(document.problems())
        .extracting(Problem::code)
        .containsOnly(ProblemCode.INVALID_CONTENT);
    List<Position> positions =
        root.tours()
            .getFirst()
            .legs()
            .getFirst()
            .shipments()
            .getFirst()
            .items()
            .getFirst()
            .positions();
    assertThat(positions).extracting(Position::count).containsExactly("1", "2");
    assertThat(positions.getFirst().label()).isNull();
    assertThat(positions)
        .extracting(position -> position.codes().getFirst().entries().getFirst().value())
        .containsExactly("v1", "v2");
    assertThat(root.tours().getFirst().legs())
        .extracting(leg -> leg.head() == null ? null : leg.head().number())
        .containsExactly(null, "T2", "T3");
  }

  @Test
  void reportsUnknownElementsAndAnUnexpectedRoot() {
    XmlDocument document = CODEC.read(List.of("<TOUR><LEG_HEAD><X/></LEG_HEAD></TOUR>"));

    assertThat(document.problems())
        .extracting(Problem::code)
        .contains(ProblemCode.UNEXPECTED_ROOT, ProblemCode.UNKNOWN_ELEMENT);
  }

  @Test
  void neverProcessesADoctype() {
    List<String> withSubset =
        List.of(
            "<?xml version=\"1.0\"?>",
            "<!DOCTYPE TOURS [",
            "  <!ENTITY secret SYSTEM \"file:///etc/passwd\">",
            "]>",
            "<TOURS><TOUR><LEG_HEAD><LEG_NO>1</LEG_NO></LEG_HEAD></TOUR></TOURS>");
    List<String> usingEntity =
        List.of(
            "<!DOCTYPE TOURS [<!ENTITY secret SYSTEM \"file:///etc/passwd\">]>",
            "<TOURS><TOUR><LEG_HEAD><LEG_NO>&secret;</LEG_NO></LEG_HEAD></TOUR></TOURS>");

    assertThat(CODEC.read(withSubset).root().name()).isEqualTo("TOURS");
    assertThatThrownBy(() -> codec(b -> b.doctype(Doctype.REJECT)).read(withSubset))
        .isInstanceOf(Dtd4jException.class)
        .hasMessage("DOCTYPE declarations are not allowed");
    assertThatThrownBy(() -> CODEC.read(usingEntity)).isInstanceOf(Dtd4jException.class);
  }

  @Test
  void readsLineBreaksAsConfigured() {
    List<String> wrapped =
        List.of(
            "<TOURS><TOUR><LEG_HEAD><LEG_NO>zwei", "  Zeilen </LEG_NO></LEG_HEAD></TOUR></TOURS>");

    assertThat(number(CODEC, wrapped)).isEqualTo("zwei  Zeilen");
    assertThat(number(codec(b -> b.lineBreaks(LineBreaks.SPACE)), wrapped))
        .isEqualTo("zwei Zeilen");
    assertThat(number(codec(b -> b.lineBreaks(LineBreaks.KEEP)), wrapped))
        .isEqualTo("zwei\n  Zeilen ");
  }

  @Test
  void reportsPropertiesWithoutElement() {
    record Extra(List<Tour> tours, String colour) {}

    XmlDocument document = CODEC.toDocument(new Extra(List.of(), "blue"));

    assertThat(document.problems())
        .extracting(Problem::code, Problem::message)
        .contains(
            tuple(
                ProblemCode.UNMAPPED_PROPERTY,
                "property colour has no element and is not written"));
  }

  @Test
  void readsAndWritesStreamsInTheEncoding() {
    Root root = root(new Head("Größe", null, new Status("1", "ü"), null));
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    CODEC.write(root, out);

    byte[] bytes = out.toByteArray();
    assertThat(new String(bytes, StandardCharsets.ISO_8859_1))
        .contains("<ID>Größe</ID>\r\n")
        .endsWith("</TOURS>\r\n");
    Root read = CODEC.bind(CODEC.read(new ByteArrayInputStream(bytes)), Root.class);
    assertThat(read.tours().getFirst().legs().getFirst().shipments().getFirst().head().id())
        .isEqualTo("Größe");
    assertThatThrownBy(() -> CODEC.write(root(new Head("1 €", null, null, null)), out))
        .isInstanceOf(Dtd4jException.class)
        .hasMessageContaining("cannot encode");
  }

  @Test
  void escapesText() {
    Root root = root(new Head("a<b>c&d]]>", null, new Status("1", null), null));

    assertThat(CODEC.write(root)).contains("        <ID>a&lt;b>c&amp;d]]&gt;</ID>");
    Root read = CODEC.read(CODEC.write(root), Root.class);
    assertThat(read.tours().getFirst().legs().getFirst().shipments().getFirst().head().id())
        .isEqualTo("a<b>c&d]]>");
  }

  @Test
  void escapesControlCharactersLikeWoodstox() {
    Root root = root(new Head("a\rb\u009fc\td", null, new Status("1", null), null));

    assertThat(CODEC.write(root)).contains("        <ID>a&#xd;b&#x9f;c\td</ID>");
    assertThatThrownBy(() -> CODEC.write(root(new Head("a\u0001", null, null, null))))
        .isInstanceOf(Dtd4jException.class)
        .hasMessageContaining("U+0001 is not allowed");
  }

  private static Root root(Head head) {
    Shipment shipment =
        new Shipment(head, List.of(new Item(List.of(new Position(null, "x", null)))));
    return new Root(List.of(new Tour(List.of(new Leg(null, List.of(shipment))))));
  }

  private static XmlCodec codec(UnaryOperator<XmlLayout.Builder> change) {
    return XmlCodec.of(change.apply(LAYOUT.toBuilder()).build());
  }

  private static String number(XmlCodec codec, List<String> lines) {
    return codec
        .read(lines)
        .root()
        .child("TOUR")
        .flatMap(tour -> tour.child("LEG_HEAD"))
        .flatMap(head -> head.child("LEG_NO"))
        .orElseThrow()
        .text();
  }
}
