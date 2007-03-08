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
package org.codehaus.xfire.aegis.inheritance.ws2.common.exception;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class AlreadyExistsException extends Exception {
    private String m_id;

    public AlreadyExistsException() {
    }

    public AlreadyExistsException(String id) {
        m_id = id;
    }

    public String getId() {
        return m_id;
    }

    public void setId(String id) {
        m_id = id;
    }

    public String toString() {
        return "[" + getClass().getName() + "] id=" + m_id;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AlreadyExistsException that = (AlreadyExistsException)o;

        if (getMessage() != null ? !getMessage().equals(that.getMessage()) : that.getMessage() != null) {
            return false;
        }

        if (m_id != null ? !m_id.equals(that.m_id) : that.m_id != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return (m_id != null ? m_id.hashCode() : 0);
    }
}
