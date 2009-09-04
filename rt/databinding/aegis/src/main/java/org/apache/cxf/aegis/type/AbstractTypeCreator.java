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
package org.apache.cxf.aegis.type;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.basic.ArrayType;
import org.apache.cxf.aegis.type.basic.ObjectType;
import org.apache.cxf.aegis.type.collection.CollectionType;
import org.apache.cxf.aegis.type.collection.MapType;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.util.ServiceUtils;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.common.util.XMLSchemaQNames;

/**
 * @author Hani Suleiman Date: Jun 14, 2005 Time: 11:59:57 PM
 */
public abstract class AbstractTypeCreator implements TypeCreator {
    public static final String HTTP_CXF_APACHE_ORG_ARRAYS = "http://cxf.apache.org/arrays";

    protected TypeMapping tm;

    protected AbstractTypeCreator nextCreator;

    private TypeCreationOptions typeConfiguration;

    private TypeCreator parent;

    public TypeMapping getTypeMapping() {
        return tm;
    }

    public TypeCreator getTopCreator() {
        TypeCreator top = this;
        TypeCreator next = top;
        while (next != null) {
            top = next;
            next = top.getParent();
        }
        return top;
    }

    public TypeCreator getParent() {
        return parent;
    }

    public void setParent(TypeCreator parent) {
        this.parent = parent;
    }

    public void setTypeMapping(TypeMapping typeMapping) {
        this.tm = typeMapping;

        if (nextCreator != null) {
            nextCreator.setTypeMapping(tm);
        }
    }

    public void setNextCreator(AbstractTypeCreator creator) {
        this.nextCreator = creator;
        nextCreator.parent = this;
    }

    public TypeClassInfo createClassInfo(Field f) {
        TypeClassInfo info = createBasicClassInfo(f.getType());
        info.setDescription("field " + f.getName() + " in  " + f.getDeclaringClass());
        return info;
    }
    
    public TypeClassInfo createBasicClassInfo(Class typeClass) {
        TypeClassInfo info = new TypeClassInfo();
        info.setDescription("class '" + typeClass.getName() + '\'');
        info.setTypeClass(typeClass);

        return info;
    }

    public AegisType createTypeForClass(TypeClassInfo info) {
        Class javaType = info.getTypeClass();
        AegisType result = null;
        boolean newType = true;

        if (info.getAegisTypeClass() != null) {
            result = createUserType(info);
        } else if (isArray(javaType)) {
            result = createArrayType(info);
        } else if (isMap(javaType)) {
            result = createMapType(info);
        }  else if (isHolder(javaType)) {
            result = createHolderType(info);
        } else if (isCollection(javaType)) {
            result = createCollectionType(info);
        } else if (isEnum(javaType)) {
            result = createEnumType(info);
        } else {
            AegisType type = getTypeMapping().getType(javaType);
            if (type == null) {
                if (info.getTypeName() != null) {
                    type = getTypeMapping().getType(info.getTypeName());
                }
                if (type == null) {
                    type = createDefaultType(info);
                } else {
                    newType = false;
                }
            } else {
                newType = false;
            }

            result = type;
        }

        if (newType
            && !getConfiguration().isDefaultNillable()) {
            result.setNillable(false);
        }

        return result;
    }


    protected boolean isHolder(Class javaType) {
        return "javax.xml.ws.Holder".equals(javaType.getName());
    }

    protected AegisType createHolderType(TypeClassInfo info) {
        if (info.getGenericType() == null) {
            throw new UnsupportedOperationException("To use holder types "
                    + "you must have an XML descriptor declaring the component type.");
        }

        Class heldCls = (Class) info.getGenericType();
        info.setTypeClass(heldCls);

        return createType(heldCls);
    }


    protected boolean isArray(Class javaType) {
        return javaType.isArray() && !javaType.equals(byte[].class);
    }

    protected AegisType createUserType(TypeClassInfo info) {
        try {
            AegisType type = info.getAegisTypeClass().newInstance();

            QName name = info.getTypeName();
            if (name == null) {
                // We do not want to use the java.lang.whatever schema type.
                // If the @ annotation or XML file didn't specify a schema type,
                // but the natural type has a schema type mapping, we use that rather
                // than create nonsense.
                if (info.getTypeClass().getPackage().getName().startsWith("java")) {
                    name = tm.getTypeQName(info.getTypeClass());
                }
                // if it's still null, we'll take our lumps, but probably end up wih
                // an invalid schema.
                if (name == null) {
                    name = createQName(info.getTypeClass());
                }
            }

            type.setSchemaType(name);
            type.setTypeClass(info.getTypeClass());
            type.setTypeMapping(getTypeMapping());

            return type;
        } catch (InstantiationException e) {
            throw new DatabindingException("Couldn't instantiate type classs " 
                                           + info.getAegisTypeClass().getName(), e);
        } catch (IllegalAccessException e) {
            throw new DatabindingException("Couldn't access type classs " 
                                           + info.getAegisTypeClass().getName(), e);
        }
    }

    protected AegisType createArrayType(TypeClassInfo info) {
        ArrayType type = new ArrayType();
        type.setTypeMapping(getTypeMapping());
        type.setTypeClass(info.getTypeClass());
        type.setSchemaType(createCollectionQName(info, type.getComponentType()));

        if (info.getMinOccurs() != -1) {
            type.setMinOccurs(info.getMinOccurs());
        } else {
            type.setMinOccurs(typeConfiguration.getDefaultMinOccurs());
        }
        
        if (info.getMaxOccurs() != -1) {
            type.setMaxOccurs(info.getMaxOccurs());
        }
        
        type.setFlat(info.isFlat());

        return type;
    }

    protected QName createQName(Class javaType) {
        String clsName = javaType.getName();

        String ns = NamespaceHelper.makeNamespaceFromClassName(clsName, "http");
        String localName = ServiceUtils.makeServiceNameFromClassName(javaType);

        return new QName(ns, localName);
    }

    protected boolean isCollection(Class javaType) {
        return Collection.class.isAssignableFrom(javaType);
    }

    protected AegisType createCollectionTypeFromGeneric(TypeClassInfo info) {
        AegisType component = getOrCreateGenericType(info);

        CollectionType type = new CollectionType(component);
        type.setTypeMapping(getTypeMapping());

        QName name = info.getTypeName();
        if (name == null) {
            name = createCollectionQName(info, component);
        }

        type.setSchemaType(name);

        type.setTypeClass(info.getTypeClass());

        if (info.getMinOccurs() != -1) {
            type.setMinOccurs(info.getMinOccurs());
        }
        if (info.getMaxOccurs() != -1) {
            type.setMaxOccurs(info.getMaxOccurs());
        }

        type.setFlat(info.isFlat());

        return type;
    }

    protected AegisType getOrCreateGenericType(TypeClassInfo info) {
        return createObjectType();
    }

    protected AegisType getOrCreateMapKeyType(TypeClassInfo info) {
        return nextCreator.getOrCreateMapKeyType(info);
    }

    protected AegisType createObjectType() {
        ObjectType type = new ObjectType();
        type.setSchemaType(XMLSchemaQNames.XSD_ANY);
        type.setTypeClass(Object.class);
        type.setTypeMapping(getTypeMapping());
        return type;
    }

    protected AegisType getOrCreateMapValueType(TypeClassInfo info) {
        return nextCreator.getOrCreateMapValueType(info);
    }

    protected AegisType createMapType(TypeClassInfo info, AegisType keyType, AegisType valueType) {
        QName schemaType = createMapQName(info, keyType, valueType);
        MapType type = new MapType(schemaType, keyType, valueType);
        type.setTypeMapping(getTypeMapping());
        type.setTypeClass(info.getTypeClass());

        return type;
    }

    protected AegisType createMapType(TypeClassInfo info) {
        AegisType keyType = getOrCreateMapKeyType(info);
        AegisType valueType = getOrCreateMapValueType(info);

        return createMapType(info, keyType, valueType);
    }

    protected QName createMapQName(TypeClassInfo info, AegisType keyType, AegisType valueType) {
        String name = keyType.getSchemaType().getLocalPart() + '2' + valueType.getSchemaType().getLocalPart()
                      + "Map";

        // TODO: Get namespace from XML?
        return new QName(tm.getMappingIdentifierURI(), name);
    }

    protected boolean isMap(Class javaType) {
        return Map.class.isAssignableFrom(javaType);
    }

    public abstract TypeClassInfo createClassInfo(PropertyDescriptor pd);

    protected boolean isEnum(Class javaType) {
        return false;
    }

    public AegisType createEnumType(TypeClassInfo info) {
        return null;
    }

    public abstract AegisType createCollectionType(TypeClassInfo info);

    public abstract AegisType createDefaultType(TypeClassInfo info);

    protected QName createCollectionQName(TypeClassInfo info, AegisType type) {
        String ns;

        if (type.isComplex()) {
            ns = type.getSchemaType().getNamespaceURI();
        } else {
            ns = tm.getMappingIdentifierURI();
        }
        if (WSDLConstants.NS_SCHEMA_XSD.equals(ns)) {
            ns = HTTP_CXF_APACHE_ORG_ARRAYS;
        }

        String first = type.getSchemaType().getLocalPart().substring(0, 1);
        String last = type.getSchemaType().getLocalPart().substring(1);
        String localName = "ArrayOf" + first.toUpperCase() + last;
        if (info.nonDefaultAttributes()) {
            localName += "-";
            if (info.getMinOccurs() >= 0) {
                localName += info.getMinOccurs();
            }
            localName += "-";
            if (info.getMaxOccurs() >= 0) {
                localName += info.getMaxOccurs();
            }
            if (info.isFlat()) {
                localName += "Flat";
            }
        }

        return new QName(ns, localName);
    }

    public abstract TypeClassInfo createClassInfo(Method m, int index);

    /**
     * Create a AegisType for a Method parameter.
     * 
     * @param m the method to create a type for
     * @param index The parameter index. If the index is less than zero, the
     *            return type is used.
     */
    public AegisType createType(Method m, int index) {
        TypeClassInfo info = createClassInfo(m, index);
        info.setDescription((index == -1 ? "return type" : "parameter " + index) + " of method "
                            + m.getName() + " in " + m.getDeclaringClass());
        return createTypeForClass(info);
    }

    public QName getElementName(Method m, int index) {
        TypeClassInfo info = createClassInfo(m, index);

        return info.getMappedName();
    }

    /**
     * Create type information for a PropertyDescriptor.
     * 
     * @param pd the propertydescriptor
     */
    public AegisType createType(PropertyDescriptor pd) {
        TypeClassInfo info = createClassInfo(pd);
        info.setDescription("property " + pd.getName());
        return createTypeForClass(info);
    }

    /**
     * Create type information for a <code>Field</code>.
     * 
     * @param f the field to create a type from
     */
    public AegisType createType(Field f) {
        TypeClassInfo info = createClassInfo(f);
        info.setDescription("field " + f.getName() + " in " + f.getDeclaringClass());
        return createTypeForClass(info);
    }
    
    /**
     * Create an Aegis type from a reflected type description.
     * This will only work for the restricted set of collection
     * types supported by Aegis. 
     * @param t the reflected type.
     * @return the type
     */
    public AegisType createType(java.lang.reflect.Type t) {
        TypeClassInfo info = new TypeClassInfo();
        info.setGenericType(t);
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            java.lang.reflect.Type rawType = pt.getRawType();
            if (rawType instanceof Class) {
                info.setTypeClass((Class)rawType);
            } 
        } else if (t instanceof Class) {
            info.setTypeClass((Class)t);
        }
        
        info.setDescription("reflected type " + t.toString());
        return createTypeForClass(info);
        
    }

    public AegisType createType(Class clazz) {
        TypeClassInfo info = createBasicClassInfo(clazz);
        info.setDescription(clazz.toString());
        return createTypeForClass(info);
    }

    public TypeCreationOptions getConfiguration() {
        return typeConfiguration;
    }

    public void setConfiguration(TypeCreationOptions tpConfiguration) {
        this.typeConfiguration = tpConfiguration;
    }
}
