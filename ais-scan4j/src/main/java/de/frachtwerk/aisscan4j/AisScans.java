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

  /** Properties of {@link AisScan} and the fields of the layout. */
  static final Map<String, String> FIELDS = new LinkedHashMap<>();

  static {
    FIELDS.put("version", "version");
    FIELDS.put("scanType", "scanart");
    FIELDS.put("relation", "relation");
    FIELDS.put("hub", "hub");
    FIELDS.put("vehicleNumber", "fahrzeugNummer");
    FIELDS.put("barcode", "barcode");
    FIELDS.put("issuer", "verursacher");
    FIELDS.put("statusCode", "statusCode");
    FIELDS.put("timestamp", "zeitstempel");
    FIELDS.put("additionalText", "zusatztext");
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
    Record record = document.records(RECORD).getFirst();

    LocalDateTime timestamp = null;
    String text = record.getString("zeitstempel");
    if (text != null) {
      try {
        timestamp = LocalDateTime.parse(text, TIMESTAMP);
      } catch (DateTimeParseException e) {
        problems.add(
            problem(
                Severity.ERROR,
                ProblemCode.INVALID_VALUE,
                "zeitstempel",
                "not a timestamp: " + text));
      }
    }

    String scanType = hasError(problems, "scanart") ? null : record.getString("scanart");
    Integer statusCode = hasError(problems, "statusCode") ? null : record.getInt("statusCode");
    if (scanType != null
        && statusCode != null
        && !allowedStatusCodes(scanType).contains(statusCode)) {
      problems.add(
          problem(
              Severity.WARNING,
              ProblemCode.UNEXPECTED_VALUE,
              "statusCode",
              "status code " + statusCode + " is not allowed for scan type " + scanType));
    }

    AisScan scan =
        new AisScan(
            hasError(problems, "version") ? null : record.getInt("version"),
            scanType,
            record.getString("relation"),
            record.getString("hub"),
            hasError(problems, "fahrzeugNummer") ? null : record.getInt("fahrzeugNummer"),
            record.getString("barcode"),
            record.getString("verursacher"),
            statusCode,
            timestamp,
            record.getString("zusatztext"));
    return new AisReadResult(scan, problems, line);
  }

  /**
   * Writes a scan as one line without trailing blanks.
   *
   * @throws FortrasException if a value does not fit the layout, e.g. an unknown scan type
   */
  public String write(AisScan scan) {
    Record record = Record.create(schema, RECORD);
    record.set("version", scan.version());
    record.set("scanart", scan.scanType());
    record.set("relation", scan.relation());
    record.set("hub", scan.hub());
    record.set("fahrzeugNummer", scan.vehicleNumber());
    record.set("barcode", scan.barcode());
    record.set("verursacher", scan.issuer());
    record.set("statusCode", scan.statusCode());
    record.set("zeitstempel", scan.timestamp() == null ? null : scan.timestamp().format(TIMESTAMP));
    record.set("zusatztext", scan.additionalText());

    WriteResult result = writer.write(schema, List.of(record));
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
