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
package org.apache.cxf.common.util.factory;

/**
 * Represents a pooling strategy that pools the data into a ThreadLocal object.
 * 
 * @author Ben Yu
 */
public class ThreadSingletonPool implements Pool {
    private static final class ThreadLocalCache extends ThreadLocal {
        protected Object initialValue() {
            return new CachingPool();
        }

        CachingPool getPool() {
            return (CachingPool)this.get();
        }
    }

    private transient ThreadLocalCache cache = new ThreadLocalCache();

    private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, java.io.IOException {
        in.defaultReadObject();
        cache = new ThreadLocalCache();
    }

    public Object getInstance(Factory factory) throws Throwable {
        return cache.getPool().getInstance(factory);
        /*
         * Object v = cache.get(); if(v==null){ v = factory.create();
         * cache.set(v); } return v;
         */
    }

    public Object getPooledInstance(Object def) {
        return cache.getPool().getPooledInstance(def);
    }

    public boolean isPooled() {
        return cache.getPool().isPooled();
    }
}
