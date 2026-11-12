package de.frachtwerk.xmlstatus4j;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.time.format.DateTimeFormatter;
import java.util.List;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * Root element {@code FAR_DATA} of an XML status file: trips ({@code FAHRT}) with shipments ({@code
 * SDG}), their status and the barcodes of their packages. Read and written with {@link
 * XmlStatusFormat}.
 */
@JsonRootName("FAR_DATA")
public class FahrDaten {
  public static final DateTimeFormatter BASIC_DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  public static final DateTimeFormatter BASIC_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("HHmm[ss]");
  public static final DateTimeFormatter BASIC_TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
  public static final DateTimeFormatter BASIC_DATE_TIME_FORMATTER_WITHOUT_SECONDS =
      DateTimeFormatter.ofPattern("yyyyMMddHHmm");

  @JacksonXmlElementWrapper(useWrapping = false)
  @JacksonXmlProperty(localName = "FAHRT")
  private List<Fahrt> fahrten;

  public List<Fahrt> getFahrten() {
    return fahrten;
  }

  public void setFahrten(List<Fahrt> fahrten) {
    this.fahrten = fahrten;
  }
}
