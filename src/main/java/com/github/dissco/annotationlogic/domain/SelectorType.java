package com.github.dissco.annotationlogic.domain;

import java.util.Map;

public enum SelectorType {

  TERM_SELECTOR("ods:TermSelector"), CLASS_SELECTOR("ods:ClassSelector"), FRAGMENT_SELECTOR(
      "oa:FragmentSelector");

  private final String selectorName;
  private static final Map<String, SelectorType> MAP = Map.of("ods:TermSelector", TERM_SELECTOR,
      "ods:ClassSelector", CLASS_SELECTOR, "ods:FragmentSelector", FRAGMENT_SELECTOR);

  SelectorType(String s) {
    this.selectorName = s;
  }

  public static SelectorType fromString(String selectorName) {
    if (MAP.containsKey(selectorName)) {
      return MAP.get(selectorName);
    }
    return null;
  }

  @Override
  public String toString() {
    return selectorName;
  }


}
