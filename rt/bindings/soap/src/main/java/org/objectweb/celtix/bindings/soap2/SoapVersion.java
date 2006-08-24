/** 
 * 
 * Copyright 2004 Protique Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.apache.cxf.bindings.soap2;

import javax.xml.namespace.QName;

/**
 * @version $Revision$
 */
public interface SoapVersion {

    double getVersion();

    String getNamespace();

    String getPrefix();

    QName getEnvelope();

    QName getHeader();

    QName getBody();
    
    QName getFault();

    String getSoapEncodingStyle();

    String getAttrNameMustUnderstand();
    
    // Role related properties
    //-------------------------------------------------------------------------
    String getAttrNameRole();
    
    String getNoneRole();

    String getUltimateReceiverRole();

    String getNextRole();
    
    String getSoapMimeType();
}
