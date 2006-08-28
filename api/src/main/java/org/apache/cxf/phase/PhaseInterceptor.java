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

package org.apache.cxf.phase;

import java.util.Set;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

/**
 * A phase interceptor participates in a PhaseInterceptorChain.
 * <pre>
 * The before and after properties contain a list of Ids that the
 * particular interceptor runs before or after.
 * </pre> 
 * @see org.apache.cxf.phase.PhaseInterceptorChain
 * @author Dan Diephouse
 */
public interface PhaseInterceptor<T extends Message> extends Interceptor<T> {

    /**
     * A Set of IDs that this interceptor needs to run after.
     * @return
     */
    Set<String> getAfter();

    /**
     * A Set of IDs that this interceptor needs to run before.
     * @return
     */
    Set<String> getBefore();

    /**
     * The ID of the interceptor.
     * @return
     */
    String getId();

    String getPhase();

}
