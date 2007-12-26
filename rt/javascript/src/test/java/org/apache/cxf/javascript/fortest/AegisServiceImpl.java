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

package org.apache.cxf.javascript.fortest;

import java.util.Collection;

/**
 * Service used to test out JavaScript talking to Aegis.
 */
public class AegisServiceImpl implements AegisService {
    private String acceptedString;
    private Collection<org.jdom.Element> acceptedCollection;
    private Collection<String> acceptedStrings;
    
    public void reset() {
        acceptedString = null;
        acceptedCollection = null;
    }
    
    /** {@inheritDoc}*/
    public void acceptAny(String before, Collection<org.jdom.Element> anything) {
        acceptedString = before;
        acceptedCollection = anything;
    }

    /**
     * @return Returns the acceptedCollection.
     */
    public Collection<org.jdom.Element> getAcceptedCollection() {
        return acceptedCollection;
    }

    /**
     * @return Returns the acceptedString.
     */
    public String getAcceptedString() {
        return acceptedString;
    }

    public void acceptStrings(Collection<String> someStrings) {
        acceptedStrings = someStrings;
    }

    /** * @return Returns the acceptedStrings.
     */
    public Collection<String> getAcceptedStrings() {
        return acceptedStrings;
    }
}
