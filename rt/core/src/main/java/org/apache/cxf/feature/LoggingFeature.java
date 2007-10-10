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
package org.apache.cxf.feature;

import org.apache.cxf.Bus;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

public class LoggingFeature extends AbstractFeature {
    private static final int DEFAULT_LIMIT = 100 * 1024;
    private static final LoggingInInterceptor IN = new LoggingInInterceptor(DEFAULT_LIMIT);
    private static final LoggingOutInterceptor OUT = new LoggingOutInterceptor(DEFAULT_LIMIT);
    
    int limit = DEFAULT_LIMIT;
    
    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        if (limit == DEFAULT_LIMIT) {
            provider.getInInterceptors().add(IN);
            provider.getInFaultInterceptors().add(IN);
            provider.getOutInterceptors().add(OUT);
            provider.getOutFaultInterceptors().add(OUT);
        } else {
            LoggingInInterceptor in = new LoggingInInterceptor(limit);
            LoggingOutInterceptor out = new LoggingOutInterceptor(limit);
            provider.getInInterceptors().add(in);
            provider.getInFaultInterceptors().add(in);
            provider.getOutInterceptors().add(out);
            provider.getOutFaultInterceptors().add(out);
        }
    }

    public void setLoggingLimit(int lim) {
        limit = lim;
    }
    
    public int getLoggingLimit() {
        return limit;
    }    
}
