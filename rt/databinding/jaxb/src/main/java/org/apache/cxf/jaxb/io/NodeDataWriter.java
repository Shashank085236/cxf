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

package org.apache.cxf.jaxb.io;

import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.jaxb.JAXBDataWriterFactory;
import org.apache.cxf.jaxb.JAXBEncoderDecoder;

public class NodeDataWriter implements DataWriter<Node> {
    final JAXBDataWriterFactory factory;
    
    public NodeDataWriter(JAXBDataWriterFactory cb) {
        factory = cb;
    }
    public void write(Object obj, Node output) {
        write(obj, null, output);
    }
    public void write(Object obj, QName elName, Node output) {
        if (obj != null) {
            JAXBEncoderDecoder.marshall(factory.getJAXBContext(),
                                        factory.getSchema(), obj,
                                        elName, output, null);
        }
    }

}
