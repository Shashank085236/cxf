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
package org.apache.cxf.resource;

import java.io.IOException;

import org.xml.sax.InputSource;

import org.apache.ws.commons.schema.resolver.URIResolver;

/**
 * Resolves URIs in a more sophisticated fashion than XmlSchema's default URI
 * Resolver does by using our own {@link org.apache.cxf.resource.URIResolver}
 * class.
 */
public class XmlSchemaURIResolver implements URIResolver {

    private org.apache.cxf.resource.URIResolver resolver;

    public XmlSchemaURIResolver() {
        try {
            resolver = new org.apache.cxf.resource.URIResolver();
        } catch (IOException e) {
            // move on...
        }
    }

    public InputSource resolveEntity(String targetNamespace, String schemaLocation, String baseUri) {
        try {
            resolver.resolveStateful(baseUri, schemaLocation, getClass());
        } catch (IOException e) {
            // move on...
        }
        if (resolver.isResolved()) {
            InputSource source = new InputSource(resolver.getInputStream());
            source.setSystemId(schemaLocation);
            return source;
        }

        return new InputSource(schemaLocation);
    }

}
