package com.kazurayam.ks.currenttestid

import static java.lang.reflect.Modifier.isPublic
import static java.lang.reflect.Modifier.isStatic
import static java.lang.reflect.Modifier.isTransient

import java.lang.reflect.Field
import java.util.stream.Collectors

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

import internal.GlobalVariable

import java.util.regex.Pattern
import java.util.regex.Matcher

public class ExpandoGlobalVariable {
	
	// https://docs.groovy-lang.org/latest/html/documentation/core-metaprogramming.html#_properties
	static final Map<String, Object> additionalProperties = Collections.synchronizedMap([:])

	private ExpandoGlobalVariable() {}

	/**
	 * inspect the 'internal.GlobalVariable' object to find the GlobalVariables contained,
	 * return the list of their names.
	 *
	 * The list will include 2 types of GlobalVariables.
	 *
	 * 1. GlobalVariables statically defined in the Execution Profile which was applied
	 *    to this time of Test Case run where the JUnit TestRunner runs. In this category,
	 *    there are 2 types of GlobalVariable.
	 *    a) GlobalVariables defined in the Profile actually applied to the test case execution.
	 *    b) GlobalVariables defined in any of Profiles which are NOT applied to the test case execution.
	 *
	 *    The a) type of GlobalVariable will have some meaningful value.
	 *    But the b) type will have 'null' value.
	 *
	 * 2. GlobalVariables dynamically added into the ExpandoGlobalVariable by calling
	 *    ExpandoGlobalVariable.addGlobalVariable(name,value)
	 */
	static SortedSet<String> keySetOfGlobalVariables() {
		Set<String> names = keySetOfStaticGlobalVariables()
		List<String> result = names.stream()
				.filter { n ->
					! additionalProperties.containsKey(n)
				}
				.collect(Collectors.toList())
		result.addAll(keySetOfAdditionalGlobalVariables())
		SortedSet<String> sorted = new TreeSet()
		sorted.addAll(result)
		return sorted
	}


	static SortedSet<String> keySetOfStaticGlobalVariables() {
		// getDelaredFields() return fields both of static and additional
		Set<Field> fields = GlobalVariable.class.getDeclaredFields() as Set<Field>
		Set<String> result = fields.stream()
				.filter { f ->
					isPublic(f.modifiers) &&
							isStatic(f.modifiers) &&
							! isTransient(f.modifiers)
				}
				.map { f -> f.getName() }
				.collect(Collectors.toSet())
		SortedSet<String> sorted = new TreeSet()
		sorted.addAll(result)
		return sorted
	}


	static SortedSet<String> keySetOfAdditionalGlobalVariables() {
		SortedSet<String> sorted = new TreeSet<String>()
		sorted.addAll(additionalProperties.keySet())
		return sorted
	}

	/**
	 * insert a public static property of type java.lang.Object
	 * into the internal.GlobalVarialbe runtime.
	 *
	 * e.g, GVH.addGlobalVariable('my_new_variable', 'foo') makes
	 * 1) internal.GlobalVariable.my_new_variable to be present and to have value 'foo'
	 * 2) internal.GlobalVariable.getMy_new_variale() to return 'foo'
	 * 3) internal.GlobalVariable.setMy_new_variable('bar') to set 'bar' as the value
	 *
	 * @param name
	 * @param value
	 */
	static int addGlobalVariable(String name, Object value) {
		// characters in the name must be valid for a Groovy variable name
		validateVariableName(name)

		// obtain the ExpandoMetaClass of the internal.GlobalVariable class
		MetaClass mc = GlobalVariable.metaClass

		// register the Getter method for the name
		String getterName = getGetterName(name)
		mc.'static'."${getterName}" = { -> return additionalProperties[name] }

		// register the Setter method for the name
		String setterName = getSetterName(name)
		mc.'static'."${setterName}" = { newValue ->
			additionalProperties[name] = newValue
		}

		// store the value into the storage
		additionalProperties.put(name, value)

		return 1
	}

	static String getGetterName(String name) {
		return 'get' + getAccessorName(name)
	}

	static String getSetterName(String name) {
		return 'set' + getAccessorName(name)
	}

	static String getAccessorName(String name) {
		return ((CharSequence)name).capitalize()
	}


	/**
	 * check if the given string is valid as a name of GlobalVaraible.
	 * 1) should not starts with digits [0-9]
	 * 2) should not starts with punctuations [$_]
	 * 3) if the 1st character is upper case, then the second character MUST NOT be lower case
	 *
	 * @param name
	 * @throws IllegalArgumentException
	 */
	static Pattern PTTN_LEADING_DIGITS = Pattern.compile('^[0-9]')
	static Pattern PTTN_LEADING_PUNCTUATIONS = Pattern.compile('^[$_]')
	static Pattern PTTN_UPPER_LOWER    = Pattern.compile('^[A-Z][a-z]')

	static void validateVariableName(String name) {
		Objects.requireNonNull(name)
		if (PTTN_LEADING_DIGITS.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with digits")
		}
		if (PTTN_LEADING_PUNCTUATIONS.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with punctuations")
		}
		if (PTTN_UPPER_LOWER.matcher(name).find()) {
			throw new IllegalArgumentException("name=${name} must not start with a uppercase letter followed by a lower case letter")
		}
	}

	/**
	 *
	 * @param entries
	 * @return the number of entries which have been dynamically added as GlobalVariable
	 */
	static int addGlobalVariables(Map<String, Object> entries) {
		int count = 0
		entries.each { entry ->
			count += addGlobalVariable(entry.key, entry.value)
		}
		return count
	}

	static void clear() {
		additionalProperties.clear()
	}

	/**
	 * @return true if GlobalVarialbe.name is defined either in 2 places
	 * 1. statically predefined in the Execution Profile
	 * 2. dynamically added by ExpandoGlobalVariable.addGlobalVariable(name, value) call
	 */
	static boolean isGlobalVariablePresent(String name) {
		return keySetOfGlobalVariables().contains(name)
	}

	static Object getGlobalVariableValue(String name) {
		if (keySetOfAdditionalGlobalVariables().contains(name)) {
			return additionalProperties[name]
		} else if (keySetOfStaticGlobalVariables().contains(name)) {
			return GlobalVariable[name]
		} else {
			return null
		}
	}

	/**
	 * If GlobaleVariable.name is present, set the value into it.
	 * Otherwise create GlobalVariable.name dynamically and set the value into it.
	 *
	 * @param name
	 * @param value
	 */
	static void ensureGlobalVariable(String name, Object value) {
		if (isGlobalVariablePresent(name)) {
			addGlobalVariable(name, value)   // will overwrite the previous value
		} else {
			addGlobalVariable(name, value)
		}
	}

	/**
	 * Create a JSON text of specified GlobalVariable and value pairs,
	 * and write the text
	 *
	 * @param nameList
	 * @param writer
	 */
	static void writeJSON(Set<String> nameList, Writer writer) {
		Objects.requireNonNull(nameList, "nameList must not be null")
		Objects.requireNonNull(writer, "writer must not be null")
		SortedMap buffer = new TreeMap<String, Object>()
		for (name in nameList) {
			if (isGlobalVariablePresent(name)) {
				buffer.put(name, getGlobalVariableValue(name))
			} else {
				;
			}
		}
		GsonBuilder gb = new GsonBuilder()
		gb.disableHtmlEscaping()
		gb.setPrettyPrinting()
		Gson gson = gb.create()
		writer.write(gson.toJson(buffer))
		writer.flush()
	}

	static String toJSON() {
		StringWriter sw = new StringWriter()
		Set<String> names = keySetOfGlobalVariables()
		writeJSON(names, sw)
		return sw.toString()
	}

	static Map<String, Object> readJSON(Set<String> names, Reader reader) {
		Objects.requireNonNull(names, "nameList must not be null")
		Objects.requireNonNull(reader, "reader must not be null")
		Map<String, Object> result = new HashMap<String, Object>()
		JsonParser jsonParser = new JsonParser()
		JsonElement jsonTree = jsonParser.parse(reader)
		if (jsonTree.isJsonObject()) {
			JsonObject jo = jsonTree.getAsJsonObject()
			for (name in names) {
				JsonElement je = jo.get(name)
				if (je != null) {
					result.put(name, je.getAsString())
				} else {
					// just ignore it
				}
			}
		} else {
			// if the input file is not a well-formed JSON text (e.g., empty string), just return an empty Map
		}
		return result
	}
}
