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

package org.apache.cxf.ws.addressing.policy;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.cxf.ws.policy.builder.primitive.NestedPrimitiveAssertion;
import org.apache.cxf.ws.policy.builder.primitive.PrimitiveAssertion;
import org.apache.neethi.Assertion;

/**
 * 
 */
public class AddressingAssertionBuilder implements AssertionBuilder {

    private static final Collection<QName> KNOWN = new ArrayList<QName>();
    private Bus bus;
    
    public AddressingAssertionBuilder(Bus b) {
        bus = b;
    }
    
    static {
        KNOWN.add(MetadataConstants.ADDRESSING_ASSERTION_QNAME);
        KNOWN.add(MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME);
        KNOWN.add(MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME);
    }
    
    public Assertion build(Element elem) {
        
        String localName = elem.getLocalName();
        QName qn = new QName(elem.getNamespaceURI(), localName);
        
        boolean optional = false;
        Attr attribute = elem.getAttributeNodeNS(PolicyConstants.getNamespace(), 
                                              PolicyConstants.getOptionalAttrName());
        if (attribute != null) {
            optional = Boolean.valueOf(attribute.getValue());
        }
        if (MetadataConstants.ADDRESSING_ASSERTION_QNAME.equals(qn)) {
            PolicyBuilder builder = bus.getExtension(PolicyBuilder.class);
            return new NestedPrimitiveAssertion(elem, builder);
        } else if (MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME.equals(qn)) {
            return new PrimitiveAssertion(MetadataConstants.ANON_RESPONSES_ASSERTION_QNAME, 
                                          optional);
        } else if (MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME.getLocalPart()
            .equals(localName)) {
            return new PrimitiveAssertion(MetadataConstants.NON_ANON_RESPONSES_ASSERTION_QNAME,
                                          optional);
        }
        return null;
    }

    public Collection<QName> getKnownElements() {
        return KNOWN;
    }

    
    
}
