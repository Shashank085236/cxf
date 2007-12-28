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

/**
 * The TypeMappingRegistry provides access to the type mappings within XFire.
 * 
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 * @since Feb 18, 2004
 */
public interface TypeMappingRegistry {
    /**
     * Register a type mapping.
     * @param namespaceURI URI that identifies the mapping. For a mapping associated with a 
     * service, it will be service's TNS. It is used as the URI for schema derived from the mapping.
     * @param mapping the type mapping object.
     * @return the previous mapping for this URI.
     */
    TypeMapping register(String namespaceURI, TypeMapping mapping);

    /**
     * register this mapping as the default mapping.
     * @param mapping
     */
    void registerDefault(TypeMapping mapping);

    /**
     * Gets the registered default <code>TypeMapping</code> instance. This
     * method returns <code>null</code> if there is no registered default
     * TypeMapping in the registry.
     * 
     * @return The registered default <code>TypeMapping</code> instance or
     *         <code>null</code>.
     */
    TypeMapping getDefaultTypeMapping();

    /**
     * Returns a list of registered encodingStyle URIs in this
     * <code>TypeMappingRegistry</code> instance.
     * 
     * @return Array of the registered encodingStyle URIs
     */
    String[] getRegisteredEncodingStyleURIs();

    /**
     * Returns the registered <code>TypeMapping</code> for the specified
     * namespace. If there is no registered <code>TypeMapping</code>
     * for the specified <code>encodingStyleURI</code>, this method returns
     * <code>null</code>.
     * 
     * @param namespaceURI the URI for the mapping.
     * @return TypeMapping for the specified encodingStyleURI or
     *         <code>null</code>
     */
    TypeMapping getTypeMapping(String namespaceURI);

    /**
     * Creates a new empty <code>TypeMapping</code> object.
     * 
     * @return TypeMapping instance.
     */
    TypeMapping createTypeMapping(boolean autoTypes);

    /**
     * Create a type mapping with the specified encodying style.
     * 
     * @param parentEncodingStyleURI Encoding style of the parent
     *            <code>TypeMapping</code> specified as an URI
     * @param autoTypes Should this mapping auto-generate types where possible
     * @return TypeMapping instance
     */
    TypeMapping createTypeMapping(String parentEncodingStyleURI, boolean autoTypes);

    /**
     * Unregisters a TypeMapping instance, if present, from the specified
     * encodingStyleURI.
     * 
     * @param encodingStyleURI Encoding style specified as an URI
     * @return <code>TypeMapping</code> instance that has been unregistered or
     *         <code>null</code> if there was no TypeMapping registered for
     *         the specified <code>encodingStyleURI</code>
     */
    TypeMapping unregisterTypeMapping(String encodingStyleURI);

    /**
     * Removes a <code>TypeMapping</code> from the TypeMappingRegistry. A
     * <code>TypeMapping</code> is associated with 1 or more
     * encodingStyleURIs. This method unregisters the specified
     * <code>TypeMapping</code> instance from all associated
     * <code>encodingStyleURIs</code> and then removes this TypeMapping
     * instance from the registry.
     * 
     * @param mapping TypeMapping to remove
     * @return <code>true</code> if specified <code>TypeMapping</code> is
     *         removed from the TypeMappingRegistry; <code>false</code> if the
     *         specified <code>TypeMapping</code> was not in the
     *         <code>TypeMappingRegistry</code>
     */
    boolean removeTypeMapping(TypeMapping mapping);

    /**
     * Removes all registered TypeMappings and encodingStyleURIs from this
     * TypeMappingRegistry.
     */
    void clear();
    
    /**
     * Set the type configuration for this type mapping registry.
     * @param configuration
     */
    void setConfiguration(Configuration configuration);
    /**
     * @return the configuration.
     */
    Configuration getConfiguration();
}
