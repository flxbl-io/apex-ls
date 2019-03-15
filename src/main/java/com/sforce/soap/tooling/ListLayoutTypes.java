package com.sforce.soap.tooling;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * This is a generated class for the SObject Enterprise API.
 * Do not edit this file, as your changes will be lost.
 */
public enum ListLayoutTypes {


  
	/**
	 * Enumeration  : SearchResult
	 */
	SearchResult("SearchResult"),

  
	/**
	 * Enumeration  : Lookup
	 */
	Lookup("Lookup"),

  
	/**
	 * Enumeration  : LookupPhone
	 */
	LookupPhone("LookupPhone"),

  
	/**
	 * Enumeration  : KeyList
	 */
	KeyList("KeyList"),

  
	/**
	 * Enumeration  : ListButtons
	 */
	ListButtons("ListButtons"),

  
	/**
	 * Enumeration  : SearchFilter
	 */
	SearchFilter("SearchFilter"),

  
	/**
	 * Enumeration  : LookupFilter
	 */
	LookupFilter("LookupFilter"),

;

	public static Map<String, String> valuesToEnums;

	static {
   		valuesToEnums = new HashMap<String, String>();
   		for (ListLayoutTypes e : EnumSet.allOf(ListLayoutTypes.class)) {
   			valuesToEnums.put(e.toString(), e.name());
   		}
   	}

   	private String value;

   	private ListLayoutTypes(String value) {
   		this.value = value;
   	}

   	@Override
   	public String toString() {
   		return value;
   	}
}