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

package de.frachtwerk.aisscan4j;

import de.frachtwerk.fortras4j.FortrasDocument;
import de.frachtwerk.fortras4j.FortrasException;
import de.frachtwerk.fortras4j.FortrasReader;
import de.frachtwerk.fortras4j.FortrasWriter;
import de.frachtwerk.fortras4j.Problem;
import de.frachtwerk.fortras4j.Record;
import de.frachtwerk.fortras4j.WriteResult;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads and writes AIS scans with the layout {@value #LAYOUT}.
 *
 * <pre>{@code
 * AisScans ais = AisScans.create();
 * AisReadResult result = ais.read(line);
 * String written = ais.write(result.scan());
 * }</pre>
 */
public final class AisScans {

  public static final String LAYOUT = "ais-scan";
  public static final String RESOURCE = "de/frachtwerk/aisscan4j/ais-scan.yaml";

  /**
   * Pattern of the timestamp in the bundled layout: day, month, year, time of day and hundredths of
   * a second.
   */
  public static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("ddMMyyyyHHmmssSS");

  private static final String RECORD = "AIS";

  /** Field names of the layout. */
  private static final class Field {
    static final String VERSION = "version";
    static final String SCAN_TYPE = "scanart";
    static final String RELATION = "relation";
    static final String HUB = "hub";
    static final String VEHICLE_NUMBER = "fahrzeugNummer";
    static final String BARCODE = "barcode";
    static final String ISSUER = "verursacher";
    static final String STATUS_CODE = "statusCode";
    static final String TIMESTAMP = "zeitstempel";
    static final String ADDITIONAL_TEXT = "zusatztext";

    private Field() {}
  }

  /** Properties of {@link AisScan} and the fields of the layout. */
  static final Map<String, String> FIELDS = new LinkedHashMap<>();

  // where property and field share the name, the field constant serves as property name too
  static {
    FIELDS.put(Field.VERSION, Field.VERSION);
    FIELDS.put("scanType", Field.SCAN_TYPE);
    FIELDS.put(Field.RELATION, Field.RELATION);
    FIELDS.put(Field.HUB, Field.HUB);
    FIELDS.put("vehicleNumber", Field.VEHICLE_NUMBER);
    FIELDS.put(Field.BARCODE, Field.BARCODE);
    FIELDS.put("issuer", Field.ISSUER);
    FIELDS.put(Field.STATUS_CODE, Field.STATUS_CODE);
    FIELDS.put("timestamp", Field.TIMESTAMP);
    FIELDS.put("additionalText", Field.ADDITIONAL_TEXT);
  }

  private final FortrasSchema schema;
  private final FortrasReader reader;
  private final FortrasWriter writer;

  private AisScans(FortrasSchema schema, FortrasReader reader, FortrasWriter writer) {
    this.schema = schema;
    this.reader = reader;
    this.writer = writer;
  }

  /** With the bundled layout. */
  public static AisScans create() {
    return of(
        FortrasSchemas.builder().addClasspath(RESOURCE, AisScans.class.getClassLoader()).build());
  }

  /** With a layout {@value #LAYOUT} of your own, e.g. a copy of the bundled layout. */
  public static AisScans of(FortrasSchemas schemas) {
    return of(schemas, LAYOUT);
  }

  /**
   * With the layout {@code layoutId} from {@code schemas}, e.g. one that extends the bundled layout
   * with {@code extends: ais-scan}. It must keep the record and field names of the bundled layout.
   */
  public static AisScans of(FortrasSchemas schemas, String layoutId) {
    return new AisScans(
        schemas.get(layoutId),
        FortrasReader.of(schemas).failOnError(false),
        FortrasWriter.create().failOnError(false));
  }

  /**
   * A copy that counts field positions and the line length in bytes of {@code charset} instead of
   * characters, e.g. for UTF-8 files whose fixed widths refer to the encoded bytes. Instances are
   * immutable and thread-safe, so one per charset can be cached.
   *
   * @throws IllegalArgumentException if the charset does not encode ASCII as single bytes
   */
  public AisScans widthsIn(Charset charset) {
    return new AisScans(schema, reader.widthsIn(charset), writer.widthsIn(charset));
  }

  /**
   * Status codes the layout allows for a scan type, from its value rule on {@code statusCode}.
   *
   * @return empty if the layout does not restrict the status code for this scan type
   */
  public Optional<Set<Integer>> statusCodesFor(String scanType) {
    return schema.rules(RECORD).stream()
        .filter(rule -> rule.field().equals(Field.STATUS_CODE))
        .findFirst()
        .flatMap(rule -> rule.allowed(Map.of(Field.SCAN_TYPE, scanType)))
        .map(AisScans::toIntegers);
  }

  private static Set<Integer> toIntegers(Set<Object> codes) {
    Set<Integer> result = new LinkedHashSet<>();
    codes.forEach(code -> result.add(((Number) code).intValue()));
    return Collections.unmodifiableSet(result);
  }

  public AisReadResult read(String line) {
    FortrasDocument document = reader.read(List.of(line), schema);
    List<Problem> problems = document.problems();
    Record rec = document.records(RECORD).getFirst();

    String scanType = hasError(problems, Field.SCAN_TYPE) ? null : rec.getString(Field.SCAN_TYPE);
    Integer statusCode =
        hasError(problems, Field.STATUS_CODE) ? null : rec.getInt(Field.STATUS_CODE);
    AisScan scan =
        new AisScan(
            hasError(problems, Field.VERSION) ? null : rec.getInt(Field.VERSION),
            scanType,
            rec.getString(Field.RELATION),
            rec.getString(Field.HUB),
            hasError(problems, Field.VEHICLE_NUMBER) ? null : rec.getInt(Field.VEHICLE_NUMBER),
            rec.getString(Field.BARCODE),
            rec.getString(Field.ISSUER),
            statusCode,
            rec.getDateTime(Field.TIMESTAMP),
            rec.getString(Field.ADDITIONAL_TEXT));
    return new AisReadResult(scan, problems, line);
  }

  /**
   * Writes a scan as one line without trailing blanks.
   *
   * @throws FortrasException if a value does not fit the layout, e.g. an unknown scan type
   */
  public String write(AisScan scan) {
    Record rec = Record.create(schema, RECORD);
    rec.set(Field.VERSION, scan.version());
    rec.set(Field.SCAN_TYPE, scan.scanType());
    rec.set(Field.RELATION, scan.relation());
    rec.set(Field.HUB, scan.hub());
    rec.set(Field.VEHICLE_NUMBER, scan.vehicleNumber());
    rec.set(Field.BARCODE, scan.barcode());
    rec.set(Field.ISSUER, scan.issuer());
    rec.set(Field.STATUS_CODE, scan.statusCode());
    rec.set(Field.TIMESTAMP, scan.timestamp());
    rec.set(Field.ADDITIONAL_TEXT, scan.additionalText());

    WriteResult result = writer.write(schema, List.of(rec));
    if (!result.isValid()) {
      throw new FortrasException("cannot write AIS scan", result.problems());
    }
    return result.lines().getFirst().stripTrailing();
  }

  private static boolean hasError(List<Problem> problems, String field) {
    return problems.stream().anyMatch(p -> p.isError() && field.equals(p.field()));
  }
}
