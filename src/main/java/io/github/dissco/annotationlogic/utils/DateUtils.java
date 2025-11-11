package io.github.dissco.annotationlogic.utils;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtils {

  private DateUtils() {
    // Utility class
  }

  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(
      "yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneOffset.UTC);

}
