# Logistics4J

Lesen und Schreiben von Dateiformaten aus der Logistik. Kern ist **fortras4j**: FORTRAS-Dateien
(und andere Formate mit fester Spaltenbreite), deren Layout – Satzarten, Feldpositionen, Typen,
Codelisten und die Struktur aus Gruppen und Häufigkeiten – vollständig in einer YAML-Datei
steht. Die Engine selbst kennt keine Satzart, kein Release und keine Kooperation.

```
fortras4j               generische Engine (einzige Abhängigkeit: Jackson 3)
ais-scan4j              AIS-Scans (eine Zeile je Scan), gelesen mit fortras4j
xml-status4j            XML-Status (FAR_DATA): Objektmodell, Lesen und Schreiben
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
  unbekannte Zeilen bleiben erhalten.
- **Fehler sammeln statt abbrechen:** Wert-, Codelisten-, Struktur- und Constraint-Verstöße
  werden als `Problem` mit stabilem `ProblemCode`, Zeile, Satzart und Feld gemeldet. Die
  Übersetzung übernimmt die Anwendung.
- **Layout-Prüfung beim Laden:** Überlappende Felder, unbekannte Typen oder Optionen, fehlende
  Codelisten und falsche Strukturverweise werden gesammelt gemeldet, jeweils mit Pfad in die Datei.

## Layout-Format

```yaml
id: bord100                     # eindeutig; zum expliziten Lesen/Schreiben
extends: fortras-bord100        # optional: erbt alles, überschreibt Einträge in records/codelists/defaults/rules
format: BORD
release: "100"
detect: "^@@PHBORD100"          # Regex auf die erste Zeile; muss eindeutig sein
maxLineLength: 512
trimTrailingSpaces: false       # neu geschriebene Zeilen rechts kürzen
passthrough: "^[A-Z]99"         # Zeilen, die ungeprüft durchgereicht werden

defaults:                        # Optionen je Typ für alle Felder des Layouts
  integer: { overflow: max }

codelists:
  adressart:
    strict: true                 # unbekannter Code: Fehler (true) oder Warnung (false)
    codes: { SHP: Versender, CON: Empfänger }   # oder als Liste: [SHP, CON]

records:
  B00:
    prefix: B00                  # Standard: die Record-Id; "" für Formate ohne Präfix
    length: 336                  # optional: Länge neu geschriebener Zeilen
    fields:
      - { name: laufendeBorderoPosition, start: 4, end: 6, type: integer }
      - { name: qualifier, start: 7, length: 3, type: code, codelist: adressart }
      - repeat: hinweise         # Block, der sich in einer Zeile wiederholt
        start: 7
        count: 6
        stride: 38
        presentIf: textschluessel   # Block zählt nur, wenn dieses Feld gefüllt ist
        fields:                  # Positionen relativ zum Block
          - { name: textschluessel, start: 1, end: 3 }
          - { name: zusatztext, start: 4, end: 38 }

rules:                           # erlaubte Werte eines Feldes, abhängig von anderen Feldern
  qualifierJeArt:                # Name; ein extends ersetzt die Regel oder entfernt sie mit null
    record: B00
    field: qualifier
    severity: warning            # error (Standard) oder warning
    cases:                       # der erste passende Fall entscheidet
      - when: { art: [A, B] }    # alle Bedingungsfelder müssen passen
        allowed: [SHP, CON]
    otherwise: [SHP]             # optional: ohne passenden Fall; fehlt es, ist alles erlaubt

structure:
  - { record: A00, as: kopf }                    # occurs: Standard "1"
  - group: sendungen
    occurs: "1..*"                               # 1, 0..1, 1..*, *, 0..9
    key: laufendeBorderoPosition                 # anderer Wert beendet die Gruppe
    constraints:
      - { first: qualifier, record: B00, values: [SHP] }
      - { required: qualifier, record: B00, values: [SHP, CON] }
      - { unique: qualifier, record: B00 }
    children:
      - { record: B00, as: adresse }
  - choice: meldungen                            # je Vorkommen genau eine Option
    occurs: "1..*"
    options:
      - group: sendungsstatus
        children: [ { record: Q10 } ]
      - { record: Q20 }
```

Positionen sind 1-basiert und inklusiv wie in den Satzartbeschreibungen. Statt `end` kann
`length` angegeben werden.

### Feldtypen

| Typ | Java-Wert | Optionen (zusätzlich zu `align`, `pad`, `null`, `overflow`) |
|---|---|---|
| `string` (Standard) | `String` | `trim` (both/right/none), `maxLength`, `valuePrefix` |
| `code` | `String` | `codelist`, `strict`, wie `string` |
| `currency` | `String` | ISO 4217 |
| `integer` | `Long` | rechtsbündig, mit Nullen aufgefüllt |
| `decimal` | `BigDecimal` | `scale` (implizite Nachkommastellen), `point: explicit` |
| `date` | `LocalDate` | `pattern` (Standard `ddMMyyyy`) |
| `time` | `LocalTime` | `pattern` (Standard `HHmm`) |
| `datetime` | `LocalDateTime` | `pattern` (Standard `ddMMyyyyHHmm`) |
| `boolean` | `Boolean` | `true`/`false`: Liste der Codes, der erste wird geschrieben |

Für alle Felder gelten außerdem `required`, `default` und `description`. Eigene Typen
registriert man mit `FortrasSchemas.builder().type(...)`.

- `null`: leerer Wert als `blanks` oder `zeros`; `date` und `time` lesen lauter Nullen immer als
  leer, `datetime` nur mit `null: zeros`
- `overflow`: zu langer Wert führt zu `error`, `truncate` (Text) oder `max` (Zahlen: lauter Neunen)
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

Positionen, Längen und `maxLineLength` zählen standardmäßig Zeichen. Beziehen sich die Spalten
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

## XML-Status (xml-status4j)

`XmlStatusFormat.read(lines)` liefert `FahrDaten` (Fahrten → Sendungen → Status und Barcodes),
`XmlStatusFormat.write(fahrDaten)` die eingerückten Zeilen mit ISO-8859-1-Deklaration. Beim Lesen
wird eine `DOCTYPE`-Deklaration ignoriert und Zeilenumbrüche in Textwerten werden entfernt;
Fehler kommen als `XmlStatusException`.

Andere Einstellungen gibt `XmlStatusConfig` vor, per Builder oder als YAML-Datei; `XmlStatusCodec`
liest und schreibt damit. Konfiguration und Codec sind unveränderlich und threadsicher:

```java
XmlStatusCodec utf8 =
    XmlStatusCodec.of(XmlStatusConfig.builder().encoding(StandardCharsets.UTF_8).build());
List<String> lines = utf8.write(fahrDaten); // <?xml version="1.0" encoding="UTF-8"?>
FahrDaten read = XmlStatusCodec.of(XmlStatusConfig.load(Path.of("status.yaml"))).read(lines);
```

```yaml
encoding: UTF-8           # Zeichensatz der Datei, Standard ISO-8859-1
declaration: auto         # auto (aus dem Zeichensatz), none oder die Deklaration selbst
indent: true              # eingerückt (Standard) oder kompakt in einer Zeile
lineBreaks: strip         # Zeilenumbrüche in Textwerten: strip (Standard), keep oder space
doctype: ignore           # DOCTYPE beim Lesen: ignore (Standard) oder reject
rootElement: FAR_DATA     # Name des Wurzelelements
elements:                 # Elementnamen je <Klasse>.<Property> des Modells
  Fahrt.sendungsdaten: SDG
```

Alle Schlüssel sind optional; unbekannte Schlüssel und ungültige Werte meldet eine
`XmlStatusConfigException` gesammelt. Die Deklaration nennt den Zeichensatz mit seinem kanonischen
Java-Namen (`UTF-8`, `IBM850`, `windows-1252`); nur ISO-8859-1 behält die Schreibweise `iso-8859-1`
der Version 0.1.0. `write` liefert Zeilen, die der Aufrufer kodiert; `read(InputStream)` und
`write(fahrDaten, OutputStream)` nutzen den konfigurierten Zeichensatz (Zeilenende CR LF).

## Build

```
mvn verify
```

Java 21.

## Lizenz

Logistics4J steht unter der GNU Lesser General Public License v3 oder (nach Wahl) einer späteren Version,
siehe [LICENSE](LICENSE). Die LGPL ergänzt die GNU General Public License v3, deren Text in
[COPYING](COPYING) liegt.
