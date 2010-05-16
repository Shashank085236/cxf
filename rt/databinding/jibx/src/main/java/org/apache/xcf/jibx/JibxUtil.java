package org.apache.xcf.jibx;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.xml.namespace.QName;

import org.jibx.runtime.Utility;

public class JibxUtil {

	public static final String SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	private static HashMap<QName, Format> simpleTypeMap = new HashMap<QName, Format>();

	private static final Class<Utility> utility = Utility.class;
	private static final Class<String> input_type = String.class;

	static {
		buildFormat("byte", "byte", "serializeByte", "parseByte", "0",
				simpleTypeMap);
		buildFormat("unsignedShort", "char", "serializeChar", "parseChar", "0",
				simpleTypeMap);
		buildFormat("double", "double", "serializeDouble", "parseDouble",
				"0.0", simpleTypeMap);
		buildFormat("float", "float", "serializeFloat", "parseFloat", "0.0",
				simpleTypeMap);
		buildFormat("int", "int", "serializeInt", "parseInt", "0",
				simpleTypeMap);
		buildFormat("long", "long", "serializeLong", "parseLong", "0",
				simpleTypeMap);
		buildFormat("short", "short", "serializeShort", "parseShort", "0",
				simpleTypeMap);
		buildFormat("boolean", "boolean", "serializeBoolean", "parseBoolean",
				"false", simpleTypeMap);
		buildFormat("dateTime", "java.util.Date", "serializeDateTime",
				"deserializeDateTime", null, simpleTypeMap);
		buildFormat("date", "java.sql.Date", "serializeSqlDate",
				"deserializeSqlDate", null, simpleTypeMap);
		buildFormat("time", "java.sql.Time", "serializeSqlTime",
				"deserializeSqlTime", null, simpleTypeMap);
		buildFormat("base64Binary", "byte[]", "serializeBase64",
				"deserializeBase64", null, simpleTypeMap);
		buildFormat("string", "java.lang.String", null, null, null,
				simpleTypeMap);
	}

	private static void buildFormat(String stype, String jtype, String sname,
			String dname, String dflt, HashMap<QName, Format> map) {
		Format format = new Format();
		format.setTypeName(jtype);
		format.setSerializeMethod(sname);
		format.setDeserializeMethod(dname);
		format.setDefaultValue(dflt);
		map.put(new QName(SCHEMA_NAMESPACE, stype), format);
	}

	public static Format getFormatElement(QName type) {
		return simpleTypeMap.get(type);
	}

	public static Object toObject(String text, QName stype) {
		Format format = simpleTypeMap.get(stype);
		if (format != null) {
			String deserializerMethod = format.getDeserializeMethod();
			if (deserializerMethod != null) {
				try {
					Method method = utility.getMethod(deserializerMethod,
							input_type);
					return method.invoke(null, new Object[] { text });
				} catch (Exception e) {
					throw new RuntimeException("", e);
				}
			}
		}
		return text;
	}

	public static String toText(Object value, QName stype) {
		Format format = simpleTypeMap.get(stype);
		if (format != null) {
			String serializeMethod = format.getSerializeMethod();
			if (serializeMethod != null) {
				String jtype = format.getTypeName();
				Class[] paraTypes = (Types.isPrimitiveType(jtype)) ? new Class[] { Types
						.getPrimitiveType(jtype) }
						: new Class[] { value.getClass() };
				try {
					Method method = utility.getMethod(serializeMethod,
							paraTypes);
					return method.invoke(null, new Object[] { value })
							.toString();
				} catch (Exception e) {
					throw new RuntimeException("", e);
				}
			}
		}
		return value.toString();
	}

	private static class Format {
		private String typeName, deserializeMethod, serializeMethod,
				defaultValue;

		public String getTypeName() {
			return typeName;
		}

		public void setTypeName(String typeName) {
			this.typeName = typeName;
		}

		public String getDeserializeMethod() {
			return deserializeMethod;
		}

		public void setDeserializeMethod(String deserializeMethod) {
			this.deserializeMethod = deserializeMethod;
		}

		public String getSerializeMethod() {
			return serializeMethod;
		}

		public void setSerializeMethod(String serializeMethod) {
			this.serializeMethod = serializeMethod;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
		}
	}
}
