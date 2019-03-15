package com.sforce.soap.tooling.metadata;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a generated class for the SObject Enterprise API.
 * Do not edit this file, as your changes will be lost.
 */
public enum SessionTimeout {


  
	/**
	 * Enumeration  : TwentyFourHours
	 */
	TwentyFourHours("TwentyFourHours"),

  
	/**
	 * Enumeration  : TwelveHours
	 */
	TwelveHours("TwelveHours"),

  
	/**
	 * Enumeration  : EightHours
	 */
	EightHours("EightHours"),

  
	/**
	 * Enumeration  : FourHours
	 */
	FourHours("FourHours"),

  
	/**
	 * Enumeration  : TwoHours
	 */
	TwoHours("TwoHours"),

  
	/**
	 * Enumeration  : SixtyMinutes
	 */
	SixtyMinutes("SixtyMinutes"),

  
	/**
	 * Enumeration  : ThirtyMinutes
	 */
	ThirtyMinutes("ThirtyMinutes"),

  
	/**
	 * Enumeration  : FifteenMinutes
	 */
	FifteenMinutes("FifteenMinutes"),

;

	public static Map<String, String> valuesToEnums;

	static {
   		valuesToEnums = new HashMap<String, String>();
   		for (SessionTimeout e : EnumSet.allOf(SessionTimeout.class)) {
   			valuesToEnums.put(e.toString(), e.name());
   		}
   	}

   	private String value;

   	private SessionTimeout(String value) {
   		this.value = value;
   	}

   	@Override
   	public String toString() {
   		return value;
   	}
}