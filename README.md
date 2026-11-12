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
  werden als `Problem` mit stabilem `ProblemCode`, Zeile, Satzart und Feld gemeldet.
  Übersetzung bleibt Sache der Anwendung.
- **Layout-Prüfung beim Start:** Überlappende Felder, unbekannte Typen oder Optionen
  (Tippfehler!), fehlende Codelisten und falsche Strukturverweise werden beim Laden gemeldet –
  alle auf einmal, mit Pfad in die Datei.

## Layout-Format

```yaml
id: bord100                     # eindeutig; zum expliziten Lesen/Schreiben
extends: fortras-bord100        # optional: erbt alles, überschreibt Einträge in records/codelists/defaults
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

Positionen sind 1-basiert und inklusiv, also genau wie in den Satzartbeschreibungen.
Statt `end` kann `length` angegeben werden.

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
| `boolean` | `Boolean` | `true`/`false`: Liste der Codes, der erste wird geschrieben |

Für alle Felder gelten außerdem `required`, `default` und `description`. Eigene Typen
registriert man mit `FortrasSchemas.builder().type(...)`.

- `null`: leerer Wert als `blanks` oder `zeros`
- `overflow`: zu langer Wert führt zu `error`, `truncate` (Text) oder `max` (Zahlen: lauter Neunen)
- `valuePrefix`: wird beim Schreiben vorangestellt und beim Lesen entfernt, z. B. ein Partnerpräfix vor Depotnummern

> **YAML-Falle:** Rein numerische Codes wie `100` müssen in Anführungszeichen stehen, sonst
> liest YAML sie als Zahl (der Loader meldet das). Kommas in Flow-Mappings `{ ... }` trennen
> Einträge; Beschreibungen mit Komma deshalb in Anführungszeichen setzen.

### Wie die Struktur zugeordnet wird

Die Zuordnung arbeitet gierig mit einer Zeile Vorausschau:

- Eine Gruppe beginnt mit einer Zeile, die sie eröffnen kann. Sie endet, wenn die nächste Zeile
  zu keinem der verbleibenden Kinder passt.
- `max = 1` begrenzt: Ein zweites B00 in `adressen` beginnt eine neue Adresse. Höhere Grenzen
  werden gezählt; Überschreitungen werden als `TOO_MANY` gemeldet, die Daten bleiben erhalten.
- Eine bekannte Zeile an falscher Stelle wird als `UNEXPECTED_RECORD` gemeldet und in der
  aktuellen Gruppe behalten, statt Folgefehler auszulösen.
- Choice-Optionen werden beim Binding in die Elterngruppe abgeflacht. Beim Schreiben aus
  Objekten entsteht deshalb die Reihenfolge der Deklaration (erst alle Optionen A, dann alle B).

## Layouts

Die Library liefert keine FORTRAS-Layouts mit; jede Anwendung bringt ihre eigenen YAML-Dateien
mit (Klassenpfad oder Verzeichnis). Kooperationsspezifisches – Partnerpräfixe, eigene Codelisten,
kürzere Nummernkreise, Pflicht-Reihenfolgen – gehört in diese Dateien oder in ein `extends`
darauf, nicht in den Code.

## AIS-Scans (ais-scan4j)

Kein FORTRAS, aber ein Format mit fester Spaltenbreite: Das Layout `ais-scan` liegt als YAML im
Modul, gelesen und geschrieben wird mit fortras4j.

```java
AisScans ais = AisScans.create();
AisReadResult result = ais.read(line);
result.problems();                      // z. B. unbekannter Hub (Warnung), Scanart (Fehler)
AisScan scan = result.scan();           // Record: scanType, hub, barcode, timestamp, ...
String out = ais.write(scan);           // leere Felder als Leerzeichen, ohne Leerzeichen am Ende
```

Welche Statuscodes zu welcher Scanart passen (`AisScans.allowedStatusCodes`), hängt von einem
anderen Feld ab und steht deshalb im Code statt im Layout; Abweichungen werden als Warnung
`UNEXPECTED_VALUE` gemeldet.

## XML-Status (xml-status4j)

`XmlStatusFormat.read(lines)` liefert `FahrDaten` (Fahrten → Sendungen → Status und Barcodes),
`XmlStatusFormat.write(fahrDaten)` die eingerückten Zeilen mit ISO-8859-1-Deklaration. Beim Lesen
wird eine `DOCTYPE`-Deklaration ignoriert und Zeilenumbrüche in Textwerten werden entfernt;
Fehler kommen als `XmlStatusException`.

## Build

```
./mvnw verify
```

Java 21.
