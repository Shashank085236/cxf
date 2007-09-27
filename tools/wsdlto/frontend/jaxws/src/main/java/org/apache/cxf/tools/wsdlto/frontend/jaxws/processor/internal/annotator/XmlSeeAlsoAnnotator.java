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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal.annotator;

import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaAnnotation;
import org.apache.cxf.tools.common.model.JavaInterface;
import org.apache.cxf.tools.util.ClassCollector;

public final class XmlSeeAlsoAnnotator implements Annotator {
    private ClassCollector collector;

    public XmlSeeAlsoAnnotator(ClassCollector c) {
        this.collector = c;
    }
    
    public void annotate(JavaAnnotatable  ja) {
        if (collector == null || collector.getTypesFactory().isEmpty()) {
            return;
        }

        JavaInterface intf = null;
        if (ja instanceof JavaInterface) {
            intf = (JavaInterface) ja;
        } else {
            throw new RuntimeException("XmlSeeAlso can only annotate JavaInterface");
        }

        JavaAnnotation jaxbAnnotation = new JavaAnnotation("XmlSeeAlso");
        intf.addImport("javax.xml.bind.annotation.XmlSeeAlso");
        
        for (String factory : collector.getTypesFactory()) {
            if ((intf.getPackageName() + ".ObjectFactory").equals(factory)) {
                jaxbAnnotation.getClassList().add("ObjectFactory");
            } else {
                jaxbAnnotation.getClassList().add(factory);
            }
        }
        intf.addAnnotation(jaxbAnnotation.toString());
    }
}

