/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.aegis.type.basic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.Aegis;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.util.XmlConstants;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.util.Base64Utility;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Type for runtime inspection of types. Looks as the class to be written, and
 * looks to see if there is a type for that class. If there is, it writes out
 * the value and inserts a <em>xsi:type</em> attribute to signal what the type
 * of the value is. Can specify an optional set of dependent <code>Type</code>'s
 * in the constructor, in the case that the type is a custom type that may not
 * have its schema in the WSDL. Can specify whether or not unknown objects
 * should be serialized as a byte stream.
 * 
 * @author <a href="mailto:peter.royal@pobox.com">peter royal</a>
 */
public class ObjectType extends Type {
    private static final QName XSI_TYPE = new QName(XmlConstants.XSI_NS, "type");
    private static final QName XSI_NIL = new QName(XmlConstants.XSI_NS, "nil");

    private Set<Type> dependencies;
    private boolean serializedWhenUnknown;
    private boolean readToDocument;

    @SuppressWarnings("unchecked")
    public ObjectType() {
        this(Collections.EMPTY_SET);
        readToDocument = true;
    }

    public ObjectType(Set<Type> dependencies) {
        this(dependencies, false);
    }

    @SuppressWarnings("unchecked")
    public ObjectType(boolean serializeWhenUnknown) {
        this(Collections.EMPTY_SET, serializeWhenUnknown);
    }

    public ObjectType(Set<Type> dependencies, boolean serializeWhenUnknown) {
        this.dependencies = dependencies;
        this.serializedWhenUnknown = serializeWhenUnknown;
    }

    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        if (isNil(reader.getAttributeReader(XSI_NIL))) {
            while (reader.hasMoreElementReaders()) {
                reader.getNextElementReader();
            }

            return null;
        }

        MessageReader typeReader = reader.getAttributeReader(XSI_TYPE);

        if (null == typeReader && !readToDocument) {
            throw new DatabindingException("Missing 'xsi:type' attribute");
        }

        String typeName = typeReader.getValue();

        if (null == typeName && !readToDocument) {
            throw new DatabindingException("Missing 'xsi:type' attribute value");
        }

        Type type = null;
        QName typeQName = null;
        if (typeName != null) {
            typeQName = extractQName(reader, typeName);
        } else {
            typeQName = reader.getName();
        }

        TypeMapping tm = (TypeMapping)context.get(Aegis.TYPE_MAPPING_KEY);
        if (tm == null) {
            tm = getTypeMapping();
        }

        type = tm.getType(typeQName);

        if (type == null) {
            type = tm.getType(getSchemaType());
        }

        if (type == null && readToDocument) {
            type = getTypeMapping().getType(Document.class);
        }

        if (null == type) {
            // TODO should check namespace as well..
            if (serializedWhenUnknown && "serializedJavaObject".equals(typeName)) {
                return reconstituteJavaObject(reader);
            }

            throw new DatabindingException("No mapped type for '" + typeName + "' (" + typeQName + ")");
        }

        return type.readObject(reader, context);
    }

    private QName extractQName(MessageReader reader, String typeName) {
        int colon = typeName.indexOf(':');

        if (-1 == colon) {
            return new QName(reader.getNamespace(), typeName);
        } else {
            return new QName(reader.getNamespaceForPrefix(typeName.substring(0, colon)), typeName
                .substring(colon + 1));
        }
    }

    private Object reconstituteJavaObject(MessageReader reader) throws DatabindingException {

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64Utility.decode(reader.getValue()));
            return new ObjectInputStream(in).readObject();
        } catch (Exception e) {
            throw new DatabindingException("Unable to reconstitute serialized object", e);
        }
    }

    private boolean isNil(MessageReader reader) {
        return null != reader && "true".equals(reader.getValue());
    }

    @Override
    public void writeObject(Object object,
                            MessageWriter writer,
                            Context context) throws DatabindingException {
        if (null == object) {
            MessageWriter nilWriter = writer.getAttributeWriter(XSI_NIL);

            nilWriter.writeValue("true");

            nilWriter.close();
        } else {
            Type type = determineType(context, object.getClass());

            if (null == type) {
                handleNullType(object, writer);
            } else {
                String prefix = writer.getPrefixForNamespace(type.getSchemaType().getNamespaceURI());

                if (null == prefix || prefix.length() == 0) {
                    addXsiType(writer, type.getSchemaType().getLocalPart());
                } else {
                    addXsiType(writer, prefix + ":" + type.getSchemaType().getLocalPart());
                }

                type.writeObject(object, writer, context);
            }
        }
    }

    private Type determineType(Context context, Class clazz) {
        TypeMapping tm = context.getTypeMapping();
        if (tm == null) {
            tm = getTypeMapping();
        }
        Type type = tm.getType(clazz);

        if (null != type) {
            return type;
        }

        Class[] interfaces = clazz.getInterfaces();

        for (int i = 0; i < interfaces.length; i++) {
            Class anInterface = interfaces[i];

            type = tm.getType(anInterface);

            if (null != type) {
                return type;
            }
        }

        Class superclass = clazz.getSuperclass();

        if (null == superclass || Object.class.equals(superclass)) {
            return null;
        }

        return determineType(context, superclass);
    }

    private void addXsiType(MessageWriter writer, String prefixedType) {
        MessageWriter typeWriter = writer.getAttributeWriter(XSI_TYPE);

        typeWriter.writeValue(prefixedType);

        typeWriter.close();
    }

    private void handleNullType(Object object, MessageWriter writer) throws DatabindingException {
        if (!serializedWhenUnknown) {
            throw new DatabindingException("Unable to write '" + object + "' [" + object.getClass().getName()
                                           + "]. Type is unknown.");
        }

        addXsiType(writer, "serializedJavaObject"); // TODO not sure what
                                                    // namespace to put
                                                    // here..should match what
                                                    // is put in writeSchema

        ByteArrayOutputStream out = new ByteArrayOutputStream(4096);

        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(out);

            objectOutputStream.writeObject(object);
            objectOutputStream.close();
        } catch (IOException e) {
            throw new DatabindingException("Unable to serialize '" + object + "' ["
                                           + object.getClass().getName() + "]", e);
        }

        writer.writeValue(Base64Utility.encode(out.toByteArray()));
    }

    public boolean isReadToDocument() {
        return readToDocument;
    }

    public void setReadToDocument(boolean readToDocument) {
        this.readToDocument = readToDocument;
    }

    public boolean isSerializedWhenUnknown() {
        return serializedWhenUnknown;
    }

    public void setSerializedWhenUnknown(boolean serializedWhenUnknown) {
        this.serializedWhenUnknown = serializedWhenUnknown;
    }

    public void setDependencies(Set<Type> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public Set<Type> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public boolean isAbstract() {
        // TODO Auto-generated method stub
        return super.isAbstract();
    }

    @Override
    public boolean isNillable() {
        // TODO Auto-generated method stub
        return super.isNillable();
    }

    @Override
    public boolean isWriteOuter() {
        // TODO Auto-generated method stub
        return super.isWriteOuter();
    }

    @Override
    public void setNillable(boolean nillable) {
        // TODO Auto-generated method stub
        super.setNillable(nillable);
    }

    @Override
    public void writeSchema(Element root) {
        if (serializedWhenUnknown) {
            Element simple = new Element("simpleType", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            simple.setAttribute(new Attribute("name", "serializedJavaObject"));
            root.addContent(simple);

            Element restriction = new Element("restriction", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            restriction.setAttribute(new Attribute("base", XmlConstants.XSD_PREFIX + ":base64Binary"));

            simple.addContent(restriction);
        }
    }
}
