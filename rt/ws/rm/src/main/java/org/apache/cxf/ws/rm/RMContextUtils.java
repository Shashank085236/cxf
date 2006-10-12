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

package org.apache.cxf.ws.rm;

import org.apache.cxf.message.Message;

/**
 * Holder for utility methods relating to contexts.
 */

public final class RMContextUtils {

    protected RMContextUtils() {
    }
    
    public static RMProperties retrieveRMProperties(Message message, boolean outbound) {
        return (RMProperties)message.get(getRMPropertiesKey(outbound));
    }
    
    public static void storeRMProperties(Message message, RMProperties rmps, boolean outbound) {
        String key = getRMPropertiesKey(outbound);
        message.put(key, rmps);
    }
    
    private static String getRMPropertiesKey(boolean outbound) {
        return outbound ? RMMessageConstants.RM_PROPERTIES_OUTBOUND 
            : RMMessageConstants.RM_PROPERTIES_INBOUND;
    }
    
}
