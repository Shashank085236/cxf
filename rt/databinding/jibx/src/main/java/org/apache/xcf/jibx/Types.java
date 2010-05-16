package org.apache.xcf.jibx;

import java.util.HashMap;

import javax.xml.namespace.QName;

/**
 * Original code was taken from org.jibx.uitl.Types.java class.
 */
public class Types {
	public static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
	public static final QName STRING_QNAME = new QName(SCHEMA_NAMESPACE,
			"string");

	private static HashMap<String, QName> s_objectTypeMap = new HashMap<String, QName>();

	static {
		s_objectTypeMap.put("java.lang.Boolean", new QName(SCHEMA_NAMESPACE,
				"boolean"));
		s_objectTypeMap.put("java.lang.Byte", new QName(SCHEMA_NAMESPACE,
				"byte"));
		s_objectTypeMap.put("java.lang.Character", new QName(SCHEMA_NAMESPACE,
				"unsignedInt"));
		s_objectTypeMap.put("java.lang.Double", new QName(SCHEMA_NAMESPACE,
				"double"));
		s_objectTypeMap.put("java.lang.Float", new QName(SCHEMA_NAMESPACE,
				"float"));
		s_objectTypeMap.put("java.lang.Integer", new QName(SCHEMA_NAMESPACE,
				"int"));
		s_objectTypeMap.put("java.lang.Long", new QName(SCHEMA_NAMESPACE,
				"long"));
		s_objectTypeMap.put("java.lang.Short", new QName(SCHEMA_NAMESPACE,
				"short"));
		s_objectTypeMap.put("java.lang.String", STRING_QNAME);
		s_objectTypeMap.put("java.math.BigDecimal", new QName(SCHEMA_NAMESPACE,
				"decimal"));
		s_objectTypeMap.put("java.math.BigInteger", new QName(SCHEMA_NAMESPACE,
				"integer"));
		s_objectTypeMap.put("java.util.Date", new QName(SCHEMA_NAMESPACE,
				"dateTime"));
		// #!j2me{
		s_objectTypeMap.put("java.sql.Date",
				new QName(SCHEMA_NAMESPACE, "date"));
		s_objectTypeMap.put("java.sql.Time",
				new QName(SCHEMA_NAMESPACE, "time"));
		s_objectTypeMap.put("java.sql.Timestamp", new QName(SCHEMA_NAMESPACE,
				"dateTime"));
		s_objectTypeMap.put("org.joda.time.LocalDate", new QName(
				SCHEMA_NAMESPACE, "date"));
		s_objectTypeMap.put("org.joda.time.DateMidnight", new QName(
				SCHEMA_NAMESPACE, "date"));
		s_objectTypeMap.put("org.joda.time.LocalTime", new QName(
				SCHEMA_NAMESPACE, "time"));
		s_objectTypeMap.put("org.joda.time.DateTime", new QName(
				SCHEMA_NAMESPACE, "dateTime"));
		// #j2me}
		s_objectTypeMap.put("byte[]", new QName(SCHEMA_NAMESPACE, "base64"));
		s_objectTypeMap.put("org.jibx.runtime.QName", new QName(
				SCHEMA_NAMESPACE, "QName"));
	}

	private static HashMap<String, QName> s_primitiveTypeMap = new HashMap<String, QName>();

	static {
		s_primitiveTypeMap.put("boolean",
				new QName(SCHEMA_NAMESPACE, "boolean"));
		s_primitiveTypeMap.put("byte", new QName(SCHEMA_NAMESPACE, "byte"));
		s_primitiveTypeMap.put("char", new QName(SCHEMA_NAMESPACE,
				"unsignedInt"));
		s_primitiveTypeMap.put("double", new QName(SCHEMA_NAMESPACE, "double"));
		s_primitiveTypeMap.put("float", new QName(SCHEMA_NAMESPACE, "float"));
		s_primitiveTypeMap.put("int", new QName(SCHEMA_NAMESPACE, "int"));
		s_primitiveTypeMap.put("long", new QName(SCHEMA_NAMESPACE, "long"));
		s_primitiveTypeMap.put("short", new QName(SCHEMA_NAMESPACE, "short"));
	}

	private static HashMap<String, Class<?>> s_wrapperMap = new HashMap<String, Class<?>>();

	static {
		s_wrapperMap.put("boolean", Boolean.TYPE);
		s_wrapperMap.put("byte", Byte.TYPE);
		s_wrapperMap.put("char", Character.TYPE);
		s_wrapperMap.put("double", Double.TYPE);
		s_wrapperMap.put("float", Float.TYPE);
		s_wrapperMap.put("int", Integer.TYPE);
		s_wrapperMap.put("long", Long.TYPE);
		s_wrapperMap.put("short", Short.TYPE);
	}

	public static boolean isSimpleValue(String type) {
		return s_primitiveTypeMap.containsKey(type)
				|| s_objectTypeMap.containsKey(type) || "void".equals(type);
	}

	public static boolean isSimpleValue(Class type) {
		return isSimpleValue(type.getName());
	}

	public static QName getSchemaType(String jtype) {
		QName stype = (QName) s_primitiveTypeMap.get(jtype);
		if (stype == null) {
			stype = (QName) s_objectTypeMap.get(jtype);
		}
		return stype;
	}

	public static boolean isPrimitiveType(String type) {
		return s_wrapperMap.containsKey(type);
	}

	public static Class<?> getPrimitiveType(String type) {
		return s_wrapperMap.get(type);
	}

}