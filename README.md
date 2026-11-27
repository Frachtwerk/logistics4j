# Logistics4J

Lesen und Schreiben von Dateiformaten aus der Logistik. Kern ist **fortras4j**: FORTRAS-Dateien
(und andere Formate mit fester Spaltenbreite), deren Layout – Satzarten, Feldpositionen, Typen,
Codelisten und die Struktur aus Gruppen und Häufigkeiten – vollständig in einer YAML-Datei
steht. Die Engine selbst kennt keine Satzart, kein Release und keine Kooperation.

```
fortras4j               generische Engine (einzige Abhängigkeit: Jackson 3)
ais-scan4j              AIS-Scans (eine Zeile je Scan), gelesen mit fortras4j
dtd4j                   XML-Dateien nach einer DTD: Lesen, Prüfen, Schreiben, Binding auf eigene Klassen
```

`groupId` aller Module ist `de.frachtwerk`, die Version wird über den Parent `logistics4j`
gemeinsam geführt.

[![Quality gate status](https://sonarcloud.io/api/project_badges/measure?project=Frachtwerk_logistics4j&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Frachtwerk_logistics4j)

## Verwendung

```java
FortrasSchemas schemas = FortrasSchemas.builder()
    .addDirectory(Path.of("/etc/fortras/layouts"))     // oder addClasspath(...)
    .build();
Fortras4j fortras = Fortras4j.of(schemas);
```

**Variante A – generisches Modell.** Das Layout wird anhand der ersten Zeile erkannt.

```java
FortrasDocument doc = fortras.read(lines);
doc.problems();                                       // Fehler und Warnungen mit Zeilennummer
for (Group sendung : doc.root().groups("sendungen")) {
  Record g00 = sendung.record("G00").orElseThrow();
  BigDecimal gewicht = g00.getDecimal("tatsaechlichesSendungsBruttoGewicht");
}
doc.records("A00").getFirst().set("borderoNummer", "4711");
List<String> out = fortras.write(doc);                // nur geänderte Felder werden neu formatiert
```

**Variante B – eigene Klassen.** Gruppen werden zu Objekten, Satzarten zu ihrem `as`-Namen,
Felder zu Properties gleichen Namens. Was öfter als einmal vorkommen darf, wird zur Liste.

```java
record Bordero(Kopf kopf, List<Sendung> sendungen) {}
record Kopf(String borderoNummer, LocalDate borderoDatum) {}
record Sendung(List<Adressen> adressen, SendungSummen summen) {}
...
Bordero b = fortras.read(lines, Bordero.class);       // wirft FortrasException bei Fehlern
List<String> out = fortras.write(b, "bord100");   // prüft die Struktur vor dem Schreiben
```

**Layouts zur Laufzeit neu laden.** Schlägt ein Reload fehl, bleiben die alten Layouts aktiv.

```java
ReloadableSchemas schemas = new ReloadableSchemas(
    () -> FortrasSchemas.builder().addDirectory(dir).build());
FortrasReader reader = FortrasReader.of(schemas);
schemas.reload();
```

## Eigenschaften

- **Verlustfreier Round-Trip:** Eine gelesene Zeile wird unverändert wieder ausgegeben;
  geänderte Felder werden in die Originalzeile eingesetzt. Nicht beschriebene Bereiche und
  unbekannte Zeilen bleiben erhalten. Zeilenenden am Ende einer übergebenen Zeile (etwa ein CR nach
  dem Trennen an LF) entfernt der Reader vorher.
- **Fehler sammeln statt abbrechen:** Wert-, Codelisten-, Struktur- und Constraint-Verstöße
  werden als `Problem` mit stabilem `ProblemCode`, Zeile, Satzart und Feld gemeldet. Die
  Übersetzung übernimmt die Anwendung.
- **Layout-Prüfung beim Laden:** Überlappende Felder, unbekannte Typen oder Optionen, fehlende
  Codelisten und falsche Strukturverweise werden gesammelt gemeldet, jeweils mit Pfad in die Datei.

## Layout-Format

Das Beispiel beschreibt ein Bordero BORD512 Release 100 vom Package-Header `@@PH` bis zum
Package-Trailer `@@PT`, mit den Pflicht-Satzarten A00, B00, D00, G00, J00 und Z00 und deren
Pflichtfeldern. Dazu kommen H00 und I00 als Beispiele für `repeat` und `rules`; die übrigen
Kann-Satzarten (A10, B10, C00, D10, E00, E10, F00, H10) werden genauso beschrieben.

```yaml
id: bord100                     # eindeutig; zum expliziten Lesen/Schreiben
# extends: bord100-basis        # optional: erbt alles, überschreibt Einträge in records/codelists/defaults/rules
format: BORD
release: "100"
detect: "^@@PHBORD100"          # Regex auf die erste Zeile; muss eindeutig sein
minLineLength: 0                # optional: kürzere Zeilen melden LINE_TOO_SHORT (0 = keine Untergrenze)
maxLineLength: 512
trimTrailingSpaces: false       # neu geschriebene Zeilen rechts kürzen
passthrough: "^[A-Z]99"         # Zeilen, die ungeprüft durchgereicht werden (bilaterale Satzarten *99)

defaults:                        # Optionen je Typ für alle Felder des Layouts
  integer: { overflow: max }

codelists:
  datenart:
    codes: [STD, KDF, ABH, CLU, GTW]
  adressart:
    strict: true                 # unbekannter Code: Fehler (true) oder Warnung (false)
    codes: { SHP: Versender, CON: Empfänger }   # oder als Liste: [SHP, CON]

records:
  PH:
    prefix: "@@PH"               # Standard: die Record-Id; "" für Formate ohne Präfix
    fields:
      - { name: dateiart, start: 5, length: 4, required: true }
      - { name: release, start: 9, length: 3, required: true }
  A00:
    fields:
      - { name: datenart, start: 4, length: 3, type: code, codelist: datenart, required: true }
      - { name: releasestand, start: 7, length: 3, required: true }
      - { name: borderoNummer, start: 10, end: 44, required: true }
      - { name: borderoDatum, start: 45, length: 8, type: date, required: true }
      - { name: codeliste, start: 59, length: 3, required: true }
      - { name: waehrung, start: 62, length: 3, type: currency, required: true }
      - { name: versenderId, start: 65, length: 35, required: true }
      - { name: empfaengerId, start: 100, length: 35, required: true }
  B00:
    length: 336                  # optional: Länge neu geschriebener Zeilen
    fields:
      - { name: laufendeBorderoPosition, start: 4, end: 6, type: integer, required: true }
      - { name: qualifier, start: 7, length: 3, type: code, codelist: adressart, required: true }
      - { name: name1, start: 10, length: 35, required: true }
      - { name: strasse, start: 45, length: 35, required: true }
      - { name: land, start: 80, length: 3, required: true }
      - { name: plz, start: 83, length: 9, required: true }
      - { name: ort, start: 92, length: 35, required: true }
  D00:
    fields:
      - { name: laufendeBorderoPosition, start: 4, length: 3, type: integer, required: true }
      - { name: sendungsposition, start: 7, length: 3, type: integer, required: true }
      - { name: wareninhalt, start: 24, length: 35, required: true }
  G00:
    fields:
      - { name: laufendeBorderoPosition, start: 4, length: 3, type: integer, required: true }
      - { name: sendungsNummer, start: 7, length: 35, required: true }
      - { name: tatsaechlichesSendungsBruttoGewicht, start: 42, length: 9, type: decimal, scale: 3, required: true }
      - { name: frankatur, start: 51, length: 3, required: true }
      - { name: direktzustellung, start: 54, length: 1, type: boolean, required: true }   # Y/N
      - { name: warengruppe, start: 182, length: 3, required: true }
  H00:
    fields:
      - { name: laufendeBorderoPosition, start: 4, length: 3, type: integer, required: true }
      - repeat: hinweise         # Block, der sich in einer Zeile wiederholt
        start: 7
        count: 6
        stride: 38
        presentIf: textschluessel   # Block zählt nur, wenn dieses Feld gefüllt ist
        fields:                  # Positionen relativ zum Block
          - { name: textschluessel, start: 1, end: 3 }
          - { name: zusatztext, start: 4, end: 38 }
  I00:
    fields:
      - { name: laufendeBorderoPosition, start: 4, length: 3, type: integer, required: true }
      - { name: leistungsart, start: 7, length: 3, required: true }
      - { name: steuerKennzeichen, start: 10, length: 1, required: true }   # 0 steuerfrei, 1 steuerpflichtig
      - { name: betrag, start: 11, length: 9, type: decimal, scale: 2, required: true }
  J00:
    fields:
      - { name: anzahlSendungen, start: 4, length: 3, type: integer, required: true }
      - { name: anzahlPackstuecke, start: 7, length: 6, type: integer, required: true }
      - { name: bruttoGewicht, start: 13, length: 9, type: decimal, scale: 3, required: true }
      - { name: anzahlGitterboxPaletten, start: 22, length: 4, type: integer, required: true }
      - { name: anzahlFlachPaletten, start: 26, length: 4, type: integer, required: true }
      - { name: zusaetzlicheFlachPaletten, start: 30, length: 4, type: integer, required: true }
      - { name: zusaetzlicheGitterboxPaletten, start: 34, length: 4, type: integer, required: true }
      - { name: summeSteuerpflichtig, start: 38, length: 11, type: decimal, scale: 2, required: true }
      - { name: summeSteuerfrei, start: 49, length: 11, type: decimal, scale: 2, required: true }
      - { name: summeNachnahmen, start: 60, length: 11, type: decimal, scale: 2, required: true }
      - { name: summeZoll, start: 71, length: 11, type: decimal, scale: 2, required: true }
      - { name: summeEinfuhrumsatzsteuer, start: 82, length: 11, type: decimal, scale: 2, required: true }
      - { name: summeMehrwertsteuer, start: 93, length: 11, type: decimal, scale: 2, required: true }
  Z00:
    fields:
      - { name: anzahlSaetze, start: 4, length: 6, type: integer, required: true }   # ohne Z00, @@PH und @@PT
      - { name: erstellungsDatum, start: 10, length: 8, type: date, required: true }
      - { name: erstellungsZeit, start: 18, length: 6, type: time, pattern: HHmmss, required: true }
  PT:
    prefix: "@@PT"               # ein Satz ohne Felder

rules:                           # erlaubte Werte eines Feldes, abhängig von anderen Feldern
  steuerJeLeistungsart:          # Name; ein extends ersetzt die Regel oder entfernt sie mit null
    record: I00
    field: steuerKennzeichen
    severity: warning            # error (Standard) oder warning
    cases:                       # der erste passende Fall entscheidet
      - when: { leistungsart: ["100", "101", "102"] }   # alle Bedingungsfelder müssen passen
        allowed: ["0"]
    otherwise: ["0", "1"]        # optional: ohne passenden Fall; fehlt es, ist alles erlaubt

structure:
  - { record: PH, as: header }                   # occurs: Standard "1"
  - { record: A00, as: kopf }
  - group: sendungen
    occurs: "1..*"                               # 1, 0..1, 1..*, *, 0..9
    key: laufendeBorderoPosition                 # anderer Wert beendet die Gruppe
    constraints:
      - { first: qualifier, record: B00, values: [SHP] }
      - { required: qualifier, record: B00, values: [SHP, CON] }
      - { unique: qualifier, record: B00 }
    children:
      - { record: B00, as: adressen, occurs: "1..*" }
      - { record: D00, as: positionen, occurs: "1..999" }
      - { record: G00, as: summen }
      - { record: H00, as: hinweise, occurs: "0..4" }
      - { record: I00, as: leistungen, occurs: "0..9" }
  - { record: J00, as: summen }
  - { record: Z00, as: kontrolle }
  - { record: PT, as: trailer }
```

Positionen sind 1-basiert und inklusiv wie in den Satzartbeschreibungen. Statt `end` kann
`length` angegeben werden. Neben `record` und `group` gibt es `choice` mit `options`: Je
Vorkommen passt genau eine der Optionen (Sätze oder Gruppen), z. B. Statusmeldungen, die
entweder einen Sendungsstatus oder einen Barcode-Status enthalten.

### Feldtypen

| Typ | Java-Wert | Optionen (zusätzlich zu `align`, `pad`, `null`, `overflow`) |
|---|---|---|
| `string` (Standard) | `String` | `trim` (both/right/none), `maxLength`, `valuePrefix`, `control` |
| `code` | `String` | `codelist`, `strict`, wie `string` |
| `currency` | `String` | ISO 4217 |
| `integer` | `Long` | rechtsbündig, mit Nullen aufgefüllt |
| `decimal` | `BigDecimal` | `scale` (implizite Nachkommastellen), `point: explicit` |
| `date` | `LocalDate` | `pattern` (Standard `ddMMyyyy`) |
| `time` | `LocalTime` | `pattern` (Standard `HHmm`) |
| `datetime` | `LocalDateTime` | `pattern` (Standard `ddMMyyyyHHmm`) |
| `boolean` | `Boolean` | `true`/`false`: Liste der Codes, der erste wird geschrieben |

Für alle Felder gelten außerdem `required`, `default` und `description`. Eigene Typen
registriert man mit `FortrasSchemas.builder().type(...)`. `date`, `time` und `datetime` lesen streng:
Einen Tag oder eine Uhrzeit, die es nicht gibt (etwa `31022026` oder `2400`), meldet der Reader als
`INVALID_VALUE`, statt sie auf einen anderen Tag zu verschieben.

- `null`: leerer Wert als `blanks` oder `zeros`; `date` liest lauter Nullen immer als leer, `time`
  und `datetime` nur mit `null: zeros` (sonst ist `0000` Mitternacht). Zahlen schreiben einen leeren
  Wert standardmäßig als Nullen (`null: zeros`), lesen lauter Nullen aber als `0`: in FORTRAS ist 0
  meist ein echter Wert
- `overflow`: zu langer Wert führt zu `error`, `truncate` (Text) oder `max` (Zahlen: lauter Neunen); Standard
  `truncate` bei `string`, sonst `error`. Ein eigenes `maxLength` gilt auch bei `truncate` als harte Grenze
- `control`: Steuerzeichen sowie Zeilen- und Absatztrenner in Textwerten beim Schreiben; `replace` (Standard)
  schreibt ein Leerzeichen (ein Zeilenumbruch wie CR LF wird zu einem), `error` meldet `INVALID_VALUE`, `keep`
  schreibt sie unverändert
- `valuePrefix`: wird beim Schreiben vorangestellt und beim Lesen entfernt, z. B. ein Partnerpräfix vor Depotnummern

> **YAML-Falle:** Rein numerische Codes wie `100` müssen in Anführungszeichen stehen, sonst
> liest YAML sie als Zahl (der Loader meldet das). Kommas in Flow-Mappings `{ ... }` trennen
> Einträge; Beschreibungen mit Komma deshalb in Anführungszeichen setzen.

### Wertregeln

Eine Regel unter `rules` schränkt die Werte eines Feldes (`field`) einer Satzart (`record`) ein,
abhängig von anderen Feldern derselben Zeile. Die Werte in der Regel werden wie Feldwerte
umgewandelt (Zahlen als Zahl, Codes als Text) und schon beim Laden geprüft. Beim Lesen wird ein
nicht erlaubter Wert als `UNEXPECTED_VALUE` mit der `severity` der Regel gemeldet und bleibt im
Datensatz. Ist das Feld oder ein Bedingungsfeld leer oder fehlerhaft, entfällt die Prüfung.
Felder in `repeat`-Blöcken sind nicht erlaubt. Welche Werte erlaubt sind, liefern
`FortrasSchema.rules(recordId)` und `ValueRule.allowed(werte)`.

### Zeichen oder Bytes

Positionen, Längen, `minLineLength` und `maxLineLength` zählen standardmäßig Zeichen. Beziehen sich die Spalten
einer UTF-8-Datei auf Bytes, belegt ein „ü“ zwei Positionen. Dafür zählen Reader und Writer auf
Wunsch in Bytes eines Charsets:

```java
FortrasReader reader = FortrasReader.of(schemas).widthsIn(StandardCharsets.UTF_8);
FortrasWriter writer = FortrasWriter.create().widthsIn(StandardCharsets.UTF_8);
```

- Lesen: Die Feldgrenzen werden auf den Bytes der Zeile bestimmt. Teilt eine Grenze ein
  Mehrbyte-Zeichen, wird `INVALID_VALUE` am Feld gemeldet; die Zeile selbst bleibt erhalten.
- Schreiben: Werte werden nach ihrer Byte-Länge aufgefüllt und gekürzt. Ein Mehrbyte-Zeichen, das
  nicht mehr passt, wird weggelassen und durch das Füllzeichen ersetzt. `overflow: error` meldet
  `VALUE_TOO_LONG` in Bytes. Unveränderte Sätze werden unverändert geschrieben.
- `maxLength` von Textfeldern zählt weiterhin Zeichen.
- Das Charset muss ASCII als einzelne Bytes kodieren (UTF-8, ISO-8859-x, windows-125x, IBM850 …);
  bei Ein-Byte-Charsets ist das Ergebnis dasselbe wie beim Zählen von Zeichen.

Reader und Writer arbeiten weiter auf Zeilen (`String`); Dekodieren und Kodieren der Datei
übernimmt der Aufrufer. Eigene Feldtypen, die Mehrbyte-Zeichen schreiben, überschreiben
`FieldCodec.format(value, length, width)`.

### Wie die Struktur zugeordnet wird

Die Zuordnung arbeitet gierig mit einer Zeile Vorausschau. Eine Gruppe endet, wenn die nächste
Zeile zu keinem ihrer verbleibenden Kinder passt. Ein Element mit `max = 1` trennt: Ein zweites
B00 in `adressen` beginnt eine neue Adresse. Zu viele Vorkommen (`TOO_MANY`) und bekannte Zeilen
an falscher Stelle (`UNEXPECTED_RECORD`) werden gemeldet, die Zeilen bleiben erhalten.

Choice-Optionen werden beim Binding in die Elterngruppe abgeflacht. Beim Schreiben aus Objekten
entsteht deshalb die Reihenfolge der Deklaration (erst alle Optionen A, dann alle B).

## Layouts

Die Library liefert keine FORTRAS-Layouts mit; jede Anwendung bringt ihre eigenen YAML-Dateien
mit (Klassenpfad oder Verzeichnis). Kooperationsspezifisches wie Partnerpräfixe, eigene Codelisten
oder Pflicht-Reihenfolgen gehört in diese Dateien oder in ein `extends` darauf.

## AIS-Scans (ais-scan4j)

Kein FORTRAS, aber ebenfalls feste Spaltenbreite: Das Layout `ais-scan` liegt als YAML im Modul
und wird mit fortras4j gelesen und geschrieben.

```java
AisScans ais = AisScans.create();
AisReadResult result = ais.read(line);
result.problems();                      // z. B. unbekannter Hub (Warnung), Scanart (Fehler)
AisScan scan = result.scan();           // Record: scanType, hub, barcode, timestamp, ...
String out = ais.write(scan);           // leere Felder als Leerzeichen, ohne Leerzeichen am Ende
```

Die erlaubten Statuscodes je Scanart stehen als Wertregel `statusCodes` im Layout
(`ais.statusCodesFor(scanType)`); Abweichungen sind Warnungen (`UNEXPECTED_VALUE`). Der
Zeitstempel ist ein `datetime` mit dem Muster `ddMMyyyyHHmmssSS` (`AisScans.TIMESTAMP`). Beides
lässt sich mit einem eigenen Layout ändern: `AisScans.of(schemas, "eigene-id")` mit einem Layout,
das per `extends: ais-scan` nur die Abweichungen enthält (Record- und Feldnamen bleiben), oder
`AisScans.of(schemas)` mit einem vollständigen Layout der Id `ais-scan`.

`ais.widthsIn(StandardCharsets.UTF_8)` liefert eine Kopie, die in Bytes zählt (siehe „Zeichen oder
Bytes“). Instanzen sind unveränderlich und threadsicher.

## XML nach einer DTD (dtd4j)

`dtd4j` kennt keine konkrete Struktur. Die Anwendung bringt eine oder mehrere DTDs und ein Layout mit
(Klassenpfad oder Verzeichnis), wie bei den Layouts von fortras4j:

```yaml
dtd: status.dtd            # eine DTD oder eine Liste, relativ zum Layout
root: FAR_DATA             # Wurzelelement
encoding: ISO-8859-1       # Zeichensatz der Datei, Standard UTF-8
declaration: auto          # auto (aus dem Zeichensatz), none oder die Deklaration selbst
indent: true               # eingerückt (Standard) oder kompakt in einer Zeile
nulls: empty               # null-Werte: leeres Element (Standard), nil (xsi:nil) oder omit
nullObjects: omit          # null-Container (Elemente mit Element-Inhalt): wie nulls (Standard) oder omit
order: dtd                 # Reihenfolge beim Schreiben von Objekten: dtd (Standard) oder alphabetical
lineBreaks: strip          # Zeilenumbrüche in Textwerten: strip (Standard), keep oder space
doctype: ignore            # DOCTYPE beim Lesen: ignore (Standard) oder reject; nie ausgewertet
validation: warning        # Abweichungen von der DTD: warning (Standard), error oder off
properties:                # Property der eigenen Klasse je Element, Standard der Elementname
  FAHRT: fahrten
  FAR: fahrtHeadDaten
groups:                    # Listen-Property für die wiederholte Gruppe im Inhaltsmodell eines Elements
  FAHRT: abschnitte        # FAHRT (FAR?, SDG+)+: je Vorkommen ein Objekt mit fahrtHeadDaten und sendungsdaten
```

```java
XmlCodec codec = XmlCodec.of(XmlLayout.loadClasspath("xml/status.yaml", loader));
XmlDocument document = codec.read(lines);        // Elemente in Dokumentreihenfolge und Problems
FahrDaten daten = codec.read(lines, FahrDaten.class);
List<String> out = codec.write(daten);           // in der Reihenfolge der DTD
```

- **Lesen** behält das Dokument vollständig (`XmlDocument`, `XmlElement`). Abweichungen von der DTD
  (Reihenfolge, fehlende oder unbekannte Elemente, Text an falscher Stelle) werden als `Problem` mit
  `ProblemCode` und Pfad gemeldet, mit der Schwere aus `validation`; die Daten bleiben erhalten.
  `read(lines, Klasse)` wirft nur bei Fehlern (`validation: error`).
- **Binding** über Jackson: Elemente mit Element-Inhalt werden zu Objekten, Textelemente zu Text,
  Elemente, die laut DTD mehrfach vorkommen dürfen, zu Listen. Jedes Vorkommen einer in `groups`
  benannten Gruppe wird ein eigenes Objekt; eine Gruppe endet, wo das Inhaltsmodell kein weiteres
  Element mehr zulässt. Ein einzelnes Element darf auch an eine Liste gebunden werden.
- **Schreiben** aus Objekten folgt dem Inhaltsmodell (`order: dtd`) oder sortiert alphabetisch je
  Element und Gruppen-Vorkommen; `toDocument(objekt)` liefert dazu die Problems, z. B. Properties ohne
  Element (`UNMAPPED_PROPERTY`). Ein `XmlDocument` wird in seiner Reihenfolge geschrieben.
- **Sicherheit:** Ein `DOCTYPE` der Datei wird nie verarbeitet, externe Entities werden nie aufgelöst;
  geprüft wird nur gegen die DTDs des Layouts.
- Nicht unterstützt: Attribute (außer `xsi:nil`), Entities, Notationen und bedingte Abschnitte in der
  DTD.

## Build

```
mvn verify
```

Java 21.

## Lizenz

Logistics4J steht unter der GNU Lesser General Public License v3 oder (nach Wahl) einer späteren Version,
siehe [LICENSE](LICENSE). Die LGPL ergänzt die GNU General Public License v3, deren Text in
[COPYING](COPYING) liegt.
