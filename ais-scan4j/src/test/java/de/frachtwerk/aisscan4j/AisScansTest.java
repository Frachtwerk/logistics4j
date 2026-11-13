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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import de.frachtwerk.fortras4j.FortrasException;
import de.frachtwerk.fortras4j.Problem;
import de.frachtwerk.fortras4j.ProblemCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AisScansTest {

  private static final AisScans AIS = AisScans.create();

  private static final String LINE =
      "100HBSBRLMUB0100340449636536225017st                                 3611010202400044900OK";

  @Test
  void readsSampleFile() throws IOException {
    String line;
    try (InputStream in = getClass().getResourceAsStream("/ais/HBS_MUB_2024101000054324.45543")) {
      line = new String(in.readAllBytes(), StandardCharsets.ISO_8859_1).strip();
    }

    AisReadResult result = AIS.read(line);

    assertThat(result.problems()).isEmpty();
    assertThat(result.scan())
        .isEqualTo(
            new AisScan(
                100,
                "HBS",
                "BRL",
                "MUB",
                1,
                "00340449636536225017",
                "st",
                361,
                LocalDateTime.of(2024, 10, 10, 0, 4, 49, 0),
                "OK"));
  }

  @Test
  void writesWhatItReads() {
    assertThat(AIS.write(AIS.read(LINE).scan())).isEqualTo(LINE);
  }

  @Test
  void unknownScanTypeIsAnError() {
    AisReadResult result = AIS.read(LINE.replace("HBS", "XYZ"));

    assertThat(result.isValid()).isFalse();
    assertThat(result.problems("scanType"))
        .extracting(Problem::code)
        .containsExactly(ProblemCode.UNKNOWN_CODE);
    assertThat(result.scan().scanType()).isNull();
  }

  @Test
  void unknownHubIsAWarningAndKept() {
    AisReadResult result = AIS.read(LINE.replace("MUB", "XXX"));

    assertThat(result.isValid()).isTrue();
    assertThat(result.problems("hub"))
        .extracting(Problem::code)
        .containsExactly(ProblemCode.UNKNOWN_CODE);
    assertThat(result.scan().hub()).isEqualTo("XXX");
  }

  @Test
  void statusCodeMustMatchScanType() {
    AisReadResult result = AIS.read(LINE.replace("HBS", "HES"));

    assertThat(result.isValid()).isTrue();
    assertThat(result.problems("statusCode"))
        .extracting(Problem::code)
        .containsExactly(ProblemCode.UNEXPECTED_VALUE);
    assertThat(AisScans.allowedStatusCodes("HES")).contains(261).doesNotContain(361);
    assertThat(AisScans.allowedStatusCodes("FBS")).containsExactlyInAnyOrder(1, 3, 5, 6, 7, 8);
  }

  @Test
  void invalidTimestampIsAnError() {
    AisReadResult result = AIS.read(LINE.replace("1010202400044900", "9999202400044900"));

    assertThat(result.problems("timestamp"))
        .extracting(Problem::code)
        .containsExactly(ProblemCode.INVALID_VALUE);
    assertThat(result.scan().timestamp()).isNull();
  }

  @Test
  void writesEmptyValuesAsBlanks() {
    AisScan scan =
        new AisScan(
            null,
            "FBS",
            null,
            null,
            null,
            "0",
            null,
            null,
            LocalDateTime.of(2026, 1, 2, 3, 4, 5, 60_000_000),
            null);

    assertThat(AIS.write(scan))
        .isEqualTo("   FBS" + " ".repeat(8) + "0" + " ".repeat(19 + 35 + 3) + "0201202603040506");
  }

  @Test
  void refusesValuesThatDoNotFit() {
    AisScan scan = new AisScan(100, "FBS", "BRL", "MUB", 123, "0", "", 1, LocalDateTime.now(), "");

    assertThatThrownBy(() -> AIS.write(scan)).isInstanceOf(FortrasException.class);
  }
}
