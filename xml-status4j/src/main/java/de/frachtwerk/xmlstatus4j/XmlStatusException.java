package de.frachtwerk.xmlstatus4j;

/** An XML status file could not be read or written. */
public class XmlStatusException extends RuntimeException {

  public XmlStatusException(String message, Throwable cause) {
    super(message, cause);
  }
}
