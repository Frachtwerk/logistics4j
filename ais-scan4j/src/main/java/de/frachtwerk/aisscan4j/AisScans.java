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
import de.frachtwerk.fortras4j.ProblemCode;
import de.frachtwerk.fortras4j.Record;
import de.frachtwerk.fortras4j.Severity;
import de.frachtwerk.fortras4j.WriteResult;
import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /** Day, month, year, time of day and hundredths of a second. */
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

  static {
    FIELDS.put("version", Field.VERSION);
    FIELDS.put("scanType", Field.SCAN_TYPE);
    FIELDS.put("relation", Field.RELATION);
    FIELDS.put("hub", Field.HUB);
    FIELDS.put("vehicleNumber", Field.VEHICLE_NUMBER);
    FIELDS.put("barcode", Field.BARCODE);
    FIELDS.put("issuer", Field.ISSUER);
    FIELDS.put("statusCode", Field.STATUS_CODE);
    FIELDS.put("timestamp", Field.TIMESTAMP);
    FIELDS.put("additionalText", Field.ADDITIONAL_TEXT);
  }

  private static final Map<String, Set<Integer>> STATUS_CODES =
      Map.of(
          "HES", Set.of(261, 271, 281, 291, 999),
          "HBS", Set.of(361, 371, 381, 391, 999),
          "LBH", Set.of(961, 971, 981, 991));
  private static final Set<Integer> OTHER_STATUS_CODES = Set.of(1, 3, 5, 6, 7, 8);

  private final FortrasSchema schema;
  private final FortrasReader reader;
  private final FortrasWriter writer = FortrasWriter.create().failOnError(false);

  private AisScans(FortrasSchemas schemas) {
    this.schema = schemas.get(LAYOUT);
    this.reader = FortrasReader.of(schemas).failOnError(false);
  }

  /** With the bundled layout. */
  public static AisScans create() {
    return of(
        FortrasSchemas.builder().addClasspath(RESOURCE, AisScans.class.getClassLoader()).build());
  }

  /** With a layout {@value #LAYOUT} of your own, e.g. one that extends the bundled layout. */
  public static AisScans of(FortrasSchemas schemas) {
    return new AisScans(schemas);
  }

  /** Status codes allowed for a scan type. */
  public static Set<Integer> allowedStatusCodes(String scanType) {
    return STATUS_CODES.getOrDefault(scanType, OTHER_STATUS_CODES);
  }

  public AisReadResult read(String line) {
    FortrasDocument document = reader.read(List.of(line), schema);
    List<Problem> problems = new ArrayList<>(document.problems());
    Record rec = document.records(RECORD).getFirst();

    LocalDateTime timestamp = null;
    String text = rec.getString(Field.TIMESTAMP);
    if (text != null) {
      try {
        timestamp = LocalDateTime.parse(text, TIMESTAMP);
      } catch (DateTimeParseException e) {
        problems.add(
            problem(
                Severity.ERROR,
                ProblemCode.INVALID_VALUE,
                Field.TIMESTAMP,
                "not a timestamp: " + text));
      }
    }

    String scanType = hasError(problems, Field.SCAN_TYPE) ? null : rec.getString(Field.SCAN_TYPE);
    Integer statusCode =
        hasError(problems, Field.STATUS_CODE) ? null : rec.getInt(Field.STATUS_CODE);
    if (scanType != null
        && statusCode != null
        && !allowedStatusCodes(scanType).contains(statusCode)) {
      problems.add(
          problem(
              Severity.WARNING,
              ProblemCode.UNEXPECTED_VALUE,
              Field.STATUS_CODE,
              "status code " + statusCode + " is not allowed for scan type " + scanType));
    }

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
            timestamp,
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
    rec.set(Field.TIMESTAMP, scan.timestamp() == null ? null : scan.timestamp().format(TIMESTAMP));
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

  private static Problem problem(
      Severity severity, ProblemCode code, String field, String message) {
    return new Problem(severity, code, 1, RECORD, field, message);
  }
}
