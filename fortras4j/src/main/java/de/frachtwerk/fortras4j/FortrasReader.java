package de.frachtwerk.fortras4j;

import de.frachtwerk.fortras4j.schema.FortrasSchema;
import de.frachtwerk.fortras4j.schema.SchemaSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reads FORTRAS files into {@link FortrasDocument}s. Immutable and thread-safe; the layouts are
 * taken from the {@link SchemaSource} on every call, so reloaded layouts apply immediately.
 */
public final class FortrasReader {

  private final SchemaSource source;
  private final boolean failOnError;

  private FortrasReader(SchemaSource source, boolean failOnError) {
    this.source = Objects.requireNonNull(source);
    this.failOnError = failOnError;
  }

  public static FortrasReader of(SchemaSource source) {
    return new FortrasReader(source, false);
  }

  /** Throw a {@link FortrasException} instead of returning a document with errors. */
  public FortrasReader failOnError(boolean failOnError) {
    return new FortrasReader(source, failOnError);
  }

  /** Detects the layout from the first line. */
  public FortrasDocument read(List<String> lines) {
    List<String> content = withoutTrailingEmptyLines(lines);
    if (content.isEmpty()) {
      throw noSchema("the file is empty");
    }
    FortrasSchema schema =
        source
            .schemas()
            .detect(content.getFirst())
            .orElseThrow(
                () -> noSchema("no layout matches the first line '" + content.getFirst() + "'"));
    return read(content, schema);
  }

  public FortrasDocument read(List<String> lines, String schemaId) {
    return read(withoutTrailingEmptyLines(lines), source.schemas().get(schemaId));
  }

  public FortrasDocument read(Path file, Charset charset) {
    try {
      return read(Files.readAllLines(file, charset));
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + file, e);
    }
  }

  public FortrasDocument read(Reader reader) {
    try (BufferedReader buffered = new BufferedReader(reader)) {
      return read(buffered.lines().toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public FortrasDocument read(List<String> lines, FortrasSchema schema) {
    List<Problem> problems = new ArrayList<>();
    List<Record> records = new ArrayList<>(lines.size());
    for (int i = 0; i < lines.size(); i++) {
      records.add(LineParser.parse(schema, lines.get(i), i + 1, problems));
    }
    Group root = new StructureMatcher(schema, records, problems).match();
    problems.sort(java.util.Comparator.comparingInt(Problem::lineNumber));
    FortrasDocument document = new FortrasDocument(schema, root, problems);
    if (failOnError && !document.isValid()) {
      throw new FortrasException("invalid " + schema.id() + " file", document.errors());
    }
    return document;
  }

  private static List<String> withoutTrailingEmptyLines(List<String> lines) {
    int end = lines.size();
    while (end > 0 && lines.get(end - 1).isEmpty()) {
      end--;
    }
    return lines.subList(0, end);
  }

  private static FortrasException noSchema(String message) {
    return new FortrasException(
        message,
        List.of(new Problem(Severity.ERROR, ProblemCode.NO_SCHEMA, 1, null, null, message)));
  }
}
