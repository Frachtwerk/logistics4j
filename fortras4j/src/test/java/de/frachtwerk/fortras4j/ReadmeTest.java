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

package de.frachtwerk.fortras4j;

import static org.assertj.core.api.Assertions.assertThat;

import de.frachtwerk.fortras4j.schema.FortrasSchemas;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Keeps the layout example of the README loadable and valid for a sample bordero. */
class ReadmeTest {

  @Test
  void loadsTheLayoutExample() throws Exception {
    assertThat(readmeSchemas().get("bord100").records())
        .containsKeys("PH", "A00", "B00", "D00", "G00", "J00", "Z00", "PT");
  }

  @Test
  void readsAndWritesASampleBorderoWithoutProblems() throws Exception {
    List<String> lines =
        Files.readAllLines(
            Path.of("src/test/resources/readme/bord100.txt"), StandardCharsets.UTF_8);
    Fortras4j fortras = Fortras4j.of(readmeSchemas());

    FortrasDocument document = fortras.read(lines);

    assertThat(document.problems()).isEmpty();
    assertThat(document.schema().id()).isEqualTo("bord100");
    Group sendung = document.root().groups("sendungen").getFirst();
    assertThat(sendung.records("B00")).hasSize(2);
    assertThat(
            sendung.record("G00").orElseThrow().getDecimal("tatsaechlichesSendungsBruttoGewicht"))
        .isEqualByComparingTo("12.5");
    assertThat(fortras.write(document)).isEqualTo(lines);
  }

  private static FortrasSchemas readmeSchemas() throws Exception {
    String readme = Files.readString(Path.of("../README.md"));
    String section = readme.substring(readme.indexOf("## Layout-Format"));
    Matcher yaml = Pattern.compile("```yaml\\n(.*?)```", Pattern.DOTALL).matcher(section);
    assertThat(yaml.find()).isTrue();
    return FortrasSchemas.builder()
        .add("README.md", new ByteArrayInputStream(yaml.group(1).getBytes(StandardCharsets.UTF_8)))
        .build();
  }
}
