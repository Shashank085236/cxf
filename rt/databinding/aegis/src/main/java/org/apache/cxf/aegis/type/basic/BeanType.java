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

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.Aegis;
import org.apache.cxf.aegis.Context;
import org.apache.cxf.aegis.DatabindingException;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.util.NamespaceHelper;
import org.apache.cxf.aegis.util.XmlConstants;
import org.apache.cxf.aegis.xml.MessageReader;
import org.apache.cxf.aegis.xml.MessageWriter;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.jdom.Attribute;
import org.jdom.Element;

/**
 * Serializes JavaBeans.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @author <a href="mailto:jack.xu.hong@gmail.com">Jack Hong</a>
 */
public class BeanType extends Type {
    private BeanTypeInfo _info;

    private boolean isInterface;

    //
    // private boolean isException = false;

    public BeanType() {
    }

    public BeanType(BeanTypeInfo info) {
        this._info = info;
        this.typeClass = info.getTypeClass();
        this.isInterface = typeClass.isInterface();
    }

    private QName getXsiType(MessageReader reader) {
        XMLStreamReader xsr = reader.getXMLStreamReader();
        String value = xsr.getAttributeValue(XmlConstants.XSI_NS, "type");
        if (value == null) {
            return null;
        } else {
            return NamespaceHelper.createQName(xsr.getNamespaceContext(), value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.codehaus.xfire.aegis.type.Type#readObject(org.codehaus.xfire.aegis.MessageReader,
     *      org.codehaus.xfire.MessageContext)
     */
    @Override
    public Object readObject(MessageReader reader, Context context) throws DatabindingException {
        BeanTypeInfo info = getTypeInfo();

        try {
            Class clazz = getTypeClass();
            Object object = null;
            InterfaceInvocationHandler delegate = null;
            boolean isProxy = false;

            if (isInterface) {
                String impl = (String)context.get(clazz.getName() + ".implementation");

                if (impl == null) {
                    delegate = new InterfaceInvocationHandler();
                    object = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {clazz},
                                                    delegate);
                    isProxy = true;
                } else {
                    try {
                        clazz = ClassLoaderUtils.loadClass(impl, getClass());
                        object = clazz.newInstance();
                    } catch (ClassNotFoundException e) {
                        throw new DatabindingException("Could not find implementation class " + impl
                                                       + " for class " + clazz.getName());
                    }
                }
            }
            // else if (isException)
            // {
            // object = createFromFault(context);
            // }
            else {
                object = clazz.newInstance();
            }

            // Read attributes
            while (reader.hasMoreAttributeReaders()) {
                MessageReader childReader = reader.getNextAttributeReader();
                QName name = childReader.getName();

                Type type = info.getType(name);

                if (type != null) {
                    Object writeObj = type.readObject(childReader, context);
                    if (isProxy) {
                        delegate.writeProperty(name.getLocalPart(), writeObj);
                    } else {
                        writeProperty(name, object, writeObj, clazz, info);
                    }
                }
            }

            // Read child elements
            while (reader.hasMoreElementReaders()) {
                MessageReader childReader = reader.getNextElementReader();
                QName name = childReader.getName();
                QName qn = getXsiType(childReader);

                BeanType parent;
                Type type = null;

                // If an xsi:type has been specified, try to look it up
                if (qn != null) {
                    type = getTypeMapping().getType(qn);
                }

                // If the xsi:type lookup didn't work or there was none, use the
                // normal Type.
                if (type == null) {
                    parent = getBeanTypeWithProperty(name);
                    if (parent != null) {
                        info = parent.getTypeInfo();
                        type = info.getType(name);
                    } else {
                        type = null;
                    }
                }

                if (type != null) {
                    if (!childReader.isXsiNil()) {
                        Object writeObj = type.readObject(childReader, context);

                        if (isProxy) {
                            delegate.writeProperty(name.getLocalPart(), writeObj);
                        } else {
                            writeProperty(name, object, writeObj, clazz, info);
                        }
                    } else {
                        if (!info.isNillable(name)) {
                            throw new DatabindingException(name.getLocalPart() + " is nil, but not nillable.");

                        }
                        childReader.readToEnd();
                    }
                } else {
                    childReader.readToEnd();
                }
            }

            return object;
        } catch (IllegalAccessException e) {
            throw new DatabindingException("Illegal access. " + e.getMessage(), e);
        } catch (InstantiationException e) {
            throw new DatabindingException("Couldn't instantiate class. " + e.getMessage(), e);
        } catch (SecurityException e) {
            throw new DatabindingException("Illegal access. " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new DatabindingException("Illegal argument. " + e.getMessage(), e);
        }
    }

    /**
     * Write the specified property to a field.
     */
    protected void writeProperty(QName name, Object object, Object property, Class impl, BeanTypeInfo info)
        throws DatabindingException {
        try {
            PropertyDescriptor desc = info.getPropertyDescriptorFromMappedName(name);

            Method m = desc.getWriteMethod();

            if (m == null) {
                if (getTypeClass().isInterface()) {
                    m = getWriteMethodFromImplClass(impl, desc);
                }

                if (m == null) {
                    throw new DatabindingException("No write method for property " + name + " in "
                                                   + object.getClass());
                }
            }

            Class propertyType = desc.getPropertyType();
            if ((property == null && !propertyType.isPrimitive()) || (property != null)) {
                m.invoke(object, new Object[] {property});
            }
        } catch (Exception e) {
            if (e instanceof DatabindingException) {
                throw (DatabindingException)e;
            }

            throw new DatabindingException("Couldn't set property " + name + " on " + object + ". "
                                           + e.getMessage(), e);
        }
    }

    /**
     * This is a hack to get the write method from the implementation class for
     * an interface.
     */
    private Method getWriteMethodFromImplClass(Class impl, PropertyDescriptor pd) throws Exception {
        String name = pd.getName();
        name = "set" + name.substring(0, 1).toUpperCase() + name.substring(1);

        return impl.getMethod(name, new Class[] {pd.getPropertyType()});
    }

    /**
     * @see org.apache.cxf.aegis.type.Type#writeObject(Object,
     *      org.apache.cxf.aegis.xml.MessageWriter,
     *      org.apache.cxf.aegis.Context)
     */
    @Override
    public void writeObject(Object object, MessageWriter writer, Context context) throws DatabindingException {
        if (object == null) {
            return;
        }

        BeanTypeInfo info = getTypeInfo();

        if (getSuperType() != null) {
            writer.writeXsiType(getSchemaType());
        }

        /*
         * TODO: Replace this method with one split into two pieces so that we
         * can front-load the attributes and traverse down the list of super
         * classes.
         */
        for (Iterator itr = info.getAttributes(); itr.hasNext();) {
            QName name = (QName)itr.next();

            Object value = readProperty(object, name);
            if (value != null) {
                Type type = getType(info, name);

                if (type == null) {
                    throw new DatabindingException("Couldn't find type for " + value.getClass()
                                                   + " for property " + name);
                }

                MessageWriter cwriter = writer.getAttributeWriter(name);

                type.writeObject(value, cwriter, context);

                cwriter.close();
            }
        }

        for (Iterator itr = info.getElements(); itr.hasNext();) {
            QName name = (QName)itr.next();

            if (info.isExtension()
                && info.getPropertyDescriptorFromMappedName(name).getReadMethod().getDeclaringClass() != info
                    .getTypeClass()) {
                continue;
            }
            Object value = readProperty(object, name);

            Type type = getType(info, name);
            type = Aegis.getWriteType(context, value, type);
            MessageWriter cwriter;

            // Write the value if it is not null.
            if (value != null) {
                cwriter = getWriter(writer, name, type);

                if (type == null) {
                    throw new DatabindingException("Couldn't find type for " + value.getClass()
                                                   + " for property " + name);
                }

                type.writeObject(value, cwriter, context);

                cwriter.close();
            } else if (info.isNillable(name)) {
                cwriter = getWriter(writer, name, type);

                // Write the xsi:nil if it is null.
                cwriter.writeXsiNil();

                cwriter.close();
            }
        }
        if (info.isExtension()) {
            Type t = getSuperType();
            if (t != null) {
                t.writeObject(object, writer, context);
            }
        }
    }

    private MessageWriter getWriter(MessageWriter writer, QName name, Type type) {
        MessageWriter cwriter;
        if (type.isAbstract()) {
            cwriter = writer.getElementWriter(name);
        } else {
            cwriter = writer.getElementWriter(name);
        }
        return cwriter;
    }

    protected Object readProperty(Object object, QName name) {
        try {
            PropertyDescriptor desc = getTypeInfo().getPropertyDescriptorFromMappedName(name);

            Method m = desc.getReadMethod();

            if (m == null) {
                throw new DatabindingException("No read method for property " + name + " in class "
                                               + object.getClass().getName());
            }

            return m.invoke(object, new Object[0]);
        } catch (Exception e) {
            throw new DatabindingException("Couldn't get property " + name + " from bean " + object, e);
        }
    }

    /**
     * @see org.apache.cxf.aegis.type.Type#writeSchema(org.jdom.Element)
     */
    @Override
    public void writeSchema(Element root) {
        BeanTypeInfo info = getTypeInfo();
        Element complex = new Element("complexType", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
        complex.setAttribute(new Attribute("name", getSchemaType().getLocalPart()));
        root.addContent(complex);

        Type sooperType = getSuperType();

        if (info.isExtension() && sooperType != null) {
            Element complexContent = new Element("complexContent", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            complex.addContent(complexContent);
            complex = complexContent;
        }

        /*
         * See Java Virtual Machine specification:
         * http://java.sun.com/docs/books/vmspec/2nd-edition/html/ClassFile.doc.html#75734
         */
        if (((info.getTypeClass().getModifiers() & Modifier.ABSTRACT) != 0)
            && !info.getTypeClass().isInterface()) {
            complex.setAttribute(new Attribute("abstract", "true"));
        }

        /*
         * Decide if we're going to extend another type. If we are going to
         * defer, then make sure that we extend the type for our superclass.
         */
        boolean isExtension = info.isExtension();

        Element dummy = complex;

        if (isExtension && sooperType != null) {

            Element extension = new Element("extension", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            complex.addContent(extension);
            QName baseType = sooperType.getSchemaType();
            extension.setAttribute(new Attribute("base", getNameWithPrefix2(extension, baseType
                .getNamespaceURI(), baseType.getLocalPart())));

            dummy = extension;
        }

        Element seq = null;

        // Write out schema for elements
        for (Iterator itr = info.getElements(); itr.hasNext();) {

            QName name = (QName)itr.next();

            if (isExtension) {
                PropertyDescriptor pd = info.getPropertyDescriptorFromMappedName(name);

                assert pd.getReadMethod() != null && pd.getWriteMethod() != null;
                if (pd.getReadMethod().getDeclaringClass() != info.getTypeClass()) {
                    continue;
                }
            }

            if (seq == null) {
                seq = new Element("sequence", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
                dummy.addContent(seq);
            }

            Element element = new Element("element", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            seq.addContent(element);

            Type type = getType(info, name);

            String nameNS = name.getNamespaceURI();
            String nameWithPrefix = getNameWithPrefix(root, nameNS, name.getLocalPart());

            String prefix = NamespaceHelper.getUniquePrefix(root, type.getSchemaType().getNamespaceURI());

            writeTypeReference(name, nameWithPrefix, element, type, prefix);
        }

        /**
         * if future proof then add <xsd:any/> element
         */
        if (info.isExtensibleElements()) {
            if (seq == null) {
                seq = new Element("sequence", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
                dummy.addContent(seq);
            }
            seq.addContent(createAnyElement());
        }

        // Write out schema for attributes
        for (Iterator itr = info.getAttributes(); itr.hasNext();) {
            QName name = (QName)itr.next();

            Element element = new Element("attribute", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
            dummy.addContent(element);

            Type type = getType(info, name);

            String nameNS = name.getNamespaceURI();
            String nameWithPrefix = getNameWithPrefix(root, nameNS, name.getLocalPart());

            String prefix = NamespaceHelper.getUniquePrefix(root, type.getSchemaType()
                .getNamespaceURI());
            element.setAttribute(new Attribute("name", nameWithPrefix));
            element.setAttribute(new Attribute("type", prefix + ':' + type.getSchemaType().getLocalPart()));
        }

        /**
         * If extensible attributes then add <xsd:anyAttribute/>
         */
        if (info.isExtensibleAttributes()) {
            dummy.addContent(createAnyAttribute());
        }
    }

    private String getNameWithPrefix(Element root, String nameNS, String localName) {
        if (!nameNS.equals(getSchemaType().getNamespaceURI())) {
            String prefix = NamespaceHelper.getUniquePrefix((Element)root.getParent(), nameNS);

            if (prefix == null || prefix.length() == 0) {
                prefix = NamespaceHelper.getUniquePrefix(root, nameNS);
            }

            return prefix + ":" + localName;
        }
        return localName;
    }

    private String getNameWithPrefix2(Element root, String nameNS, String localName) {
        String prefix = NamespaceHelper.getUniquePrefix((Element)root.getParent(), nameNS);

        if (prefix == null || prefix.length() == 0) {
            prefix = NamespaceHelper.getUniquePrefix(root, nameNS);
        }

        return prefix + ":" + localName;
    }

    private Type getType(BeanTypeInfo info, QName name) {
        Type type = info.getType(name);

        if (type == null) {
            throw new NullPointerException("Couldn't find type for" + name + " in class "
                                           + getTypeClass().getName());
        }

        return type;
    }

    private void writeTypeReference(QName name, String nameWithPrefix, Element element, Type type,
                                    String prefix) {
        if (type.isAbstract()) {
            element.setAttribute(new Attribute("name", nameWithPrefix));
            element.setAttribute(new Attribute("type", prefix + ':' + type.getSchemaType().getLocalPart()));

            int minOccurs = getTypeInfo().getMinOccurs(name);
            if (minOccurs != 1) {
                element.setAttribute(new Attribute("minOccurs", new Integer(minOccurs).toString()));
            }

            if (getTypeInfo().isNillable(name)) {
                element.setAttribute(new Attribute("nillable", "true"));
            }
        } else {
            element.setAttribute(new Attribute("ref", prefix + ':' + type.getSchemaType().getLocalPart()));
        }
    }

    @Override
    public void setTypeClass(Class typeClass) {
        super.setTypeClass(typeClass);

        isInterface = typeClass.isInterface();
        // isException = Exception.class.isAssignableFrom(typeClass);
    }

    /**
     * We need to write a complex type schema for Beans, so return true.
     * 
     * @see org.apache.cxf.aegis.type.Type#isComplex()
     */
    @Override
    public boolean isComplex() {
        return true;
    }

    @Override
    public Set<Type> getDependencies() {
        Set<Type> deps = new HashSet<Type>();

        BeanTypeInfo info = getTypeInfo();

        for (Iterator itr = info.getAttributes(); itr.hasNext();) {
            QName name = (QName)itr.next();
            deps.add(info.getType(name));
        }

        for (Iterator itr = info.getElements(); itr.hasNext();) {
            QName name = (QName)itr.next();
            if (info.isExtension()
                && info.getPropertyDescriptorFromMappedName(name).getReadMethod().getDeclaringClass() != info
                    .getTypeClass()) {
                continue;
            }
            deps.add(info.getType(name));
        }

        /*
         * Automagically add chain of superclasses *if* this is an an extension.
         */
        if (info.isExtension()) {
            Type sooperType = getSuperType();
            if (sooperType != null) {
                deps.add(sooperType);
            }
        }

        return deps;
    }

    private BeanType getBeanTypeWithProperty(QName name) {
        BeanType sooper = this;
        Type type = null;

        while (type == null && sooper != null) {
            type = sooper.getTypeInfo().getType(name);

            if (type == null) {
                sooper = sooper.getSuperType();
            }
        }

        return sooper;
    }

    private BeanType getSuperType() {
        BeanTypeInfo info = getTypeInfo();
        Class c = info.getTypeClass().getSuperclass();
        /*
         * Don't dig any deeper than Object or Exception
         */
        if (c != null && c != Object.class && c != Exception.class && c != RuntimeException.class) {
            TypeMapping tm = info.getTypeMapping();
            BeanType superType = (BeanType)tm.getType(c);
            if (superType == null) {
                superType = (BeanType)getTypeMapping().getTypeCreator().createType(c);
                Class cParent = c.getSuperclass();
                if (cParent != null && cParent != Object.class) {
                    superType.getTypeInfo().setExtension(true);
                }
                tm.register(superType);
            }
            return superType;
        } else {
            return null;
        }
    }

    public BeanTypeInfo getTypeInfo() {
        if (_info == null) {
            _info = createTypeInfo();
        }

        // Delay initialization so things work in recursive scenarios
        // (XFIRE-117)
        if (!_info.isInitialized()) {
            _info.initialize();
        }

        return _info;
    }

    public BeanTypeInfo createTypeInfo() {
        BeanTypeInfo info = new BeanTypeInfo(getTypeClass(), getSchemaType().getNamespaceURI());

        info.setTypeMapping(getTypeMapping());

        return info;
    }

    /**
     * Create an element to represent any future elements that might get added
     * to the schema <xsd:any minOccurs="0" maxOccurs="unbounded"/>
     * 
     * @return
     */
    private Element createAnyElement() {
        Element result = new Element("any", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
        result.setAttribute(new Attribute("minOccurs", "0"));
        result.setAttribute(new Attribute("maxOccurs", "unbounded"));
        return result;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(": [class=");
        Class c = getTypeClass();
        sb.append((c == null) ? ("<null>") : (c.getName()));
        sb.append(",\nQName=");
        QName q = getSchemaType();
        sb.append((q == null) ? ("<null>") : (q.toString()));
        sb.append(",\ninfo=");
        sb.append(getTypeInfo().toString());
        sb.append("]");
        return sb.toString();
    }

    /**
     * Create an element to represent any future attributes that might get added
     * to the schema <xsd:anyAttribute/>
     * 
     * @return
     */
    private Element createAnyAttribute() {
        Element result = new Element("anyAttribute", XmlConstants.XSD_PREFIX, XmlConstants.XSD);
        return result;
    }

}
