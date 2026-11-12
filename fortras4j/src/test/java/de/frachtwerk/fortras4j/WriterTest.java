package de.frachtwerk.fortras4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WriterTest {

  private static final FortrasSchema SCHEMA = Demo.SCHEMAS.get("demo");

  @Test
  void writesNewRecordsWithDefaultsAndPadding() {
    Record header = Record.create(SCHEMA, "HDR").set("created", "2026-11-12");
    Record order =
        Record.create(SCHEMA, "ORD")
            .set("orderNo", 7)
            .set("colour", "GRN")
            .set("weight", "3.456")
            .set("express", false);
    Record item =
        Record.create(SCHEMA, "ITM")
            .set("orderNo", 7)
            .set("notes", List.of(Map.of("key", "N1", "text", "ab")));
    Record end = Record.create(SCHEMA, "END").set("count", 3);

    WriteResult result = FortrasWriter.create().write(SCHEMA, List.of(header, order, item, end));

    assertThat(result.lines())
        .containsExactly(
            "HDR00112112026", "ORD007GRN000346N", "ITM007 N1ab              ", "END0003");
    StringWriter out = new StringWriter();
    result.writeTo(out, "\r\n");
    assertThat(out.toString()).startsWith("HDR00112112026\r\nORD007");
  }

  @Test
  void overlaysOnlyChangedFieldsOntoTheOriginalLine() {
    List<String> lines = List.of("HDR00112112026 free text", "ORD001RED001250Y", "END0001");
    FortrasDocument document = FortrasReader.of(Demo.SCHEMAS).read(lines);

    document.records("ORD").getFirst().set("colour", "GRN");

    assertThat(FortrasWriter.create().write(document).lines())
        .containsExactly("HDR00112112026 free text", "ORD001GRN001250Y", "END0001");
  }

  @Test
  void validatesStructureBeforeWriting() {
    Record header = Record.create(SCHEMA, "HDR").set("created", "2026-11-12");

    assertThatThrownBy(() -> FortrasWriter.create().write(SCHEMA, List.of(header)))
        .isInstanceOf(FortrasException.class)
        .hasMessageContaining("MISSING");

    WriteResult lenient = FortrasWriter.create().failOnError(false).write(SCHEMA, List.of(header));
    assertThat(lenient.isValid()).isFalse();
    assertThat(lenient.lines()).containsExactly("HDR00112112026");
  }

  @Test
  void reportsValuesThatDoNotFit() {
    Record order = Record.create(SCHEMA, "ORD").set("orderNo", 1000);
    List<Record> records =
        List.of(
            Record.create(SCHEMA, "HDR").set("created", "2026-11-12"),
            order,
            Record.create(SCHEMA, "END"));

    assertThatThrownBy(() -> FortrasWriter.create().write(SCHEMA, records))
        .isInstanceOf(FortrasException.class)
        .satisfies(e ->
            assertThat(((FortrasException) e).problems())
                .extracting(Problem::code, Problem::field)
                .containsExactly(
                    org.assertj.core.groups.Tuple.tuple(ProblemCode.VALUE_TOO_LONG, "orderNo")));
  }

  @Test
  void rejectsUnknownFieldsAndUnconvertibleValues() {
    Record order = Record.create(SCHEMA, "ORD");

    assertThatThrownBy(() -> order.set("weigth", 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no field 'weigth'");
    assertThatThrownBy(() -> order.set("orderNo", "1.5"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ORD.orderNo");
  }
}
