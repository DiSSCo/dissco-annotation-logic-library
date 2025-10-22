package eu.dissco.annotationlogic.domain;

import java.util.Map;

public enum SelectorType {

  TERM_SELECTOR("ods:TermSelector"), CLASS_SELECTOR("ods:ClassSelector"), FRAGMENT_SELECTOR(
      "oa:FragmentSelector");

  private final String state;
  private static final Map<String, SelectorType> MAP = Map.of("ods:TermSelector", TERM_SELECTOR,
      "ods:ClassSelector", CLASS_SELECTOR, "ods:FragmentSelector", FRAGMENT_SELECTOR);

  SelectorType(String s) {
    this.state = s;
  }

  public static SelectorType fromString(String name) {
    if (MAP.containsKey(name)) {
      return MAP.get(name);
    }
    return null;
  }

  @Override
  public String toString() {
    return state;
  }


}
