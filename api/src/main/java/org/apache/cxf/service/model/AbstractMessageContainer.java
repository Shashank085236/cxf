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

package org.apache.cxf.service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;


public abstract class AbstractMessageContainer extends AbstractPropertiesHolder {
    protected QName mName;
    private OperationInfo operation;
    private Map<QName, MessagePartInfo> messageParts 
        = new LinkedHashMap<QName, MessagePartInfo>(4);
    
    /**
     * Initializes a new instance of the <code>MessagePartContainer</code>.
     * @param operation the operation.
     * @param nm 
     */
    AbstractMessageContainer(OperationInfo op, QName nm) {
        operation = op;
        mName = nm;
    }

    public QName getName() {
        return mName;
    }
    
    
    /**
     * Returns the operation of this container.
     *
     * @return the operation.
     */
    public OperationInfo getOperation() {
        return operation;
    }

    /**
     * Adds a message part to this container.
     *
     * @param name  the qualified name of the message part
     * @return name  the newly created <code>MessagePartInfo</code> object
     */
    public MessagePartInfo addMessagePart(QName name) {
        if (name == null) {
            throw new IllegalArgumentException("Invalid name [" + name + "]");
        }

        MessagePartInfo part = new MessagePartInfo(name, this);
        addMessagePart(part);
        return part;
    }
    
    public QName getMessagePartQName(String name) {
        return new QName(this.getOperation().getInterface().getService().getTargetNamespace(), name);
    }
    
    public MessagePartInfo addMessagePart(String name) {
        return addMessagePart(getMessagePartQName(name));
    }    
    /**
     * Adds a message part to this container.
     *
     * @param part the message part.
     */
    public void addMessagePart(MessagePartInfo part) {
        part.setIndex(messageParts.size());
        messageParts.put(part.getName(), part);
    }

    public int getMessagePartIndex(MessagePartInfo part) {
        int i = 0;
        for (MessagePartInfo p : messageParts.values()) {
            if (part == p) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public MessagePartInfo getMessagePartByIndex(int i) {
        for (MessagePartInfo p : messageParts.values()) {
            if (p.getIndex() == i) {
                return p;
            }
        }
        return null;
    }

    /**
     * Removes an message part from this container.
     *
     * @param name the qualified message part name.
     */
    public void removeMessagePart(QName name) {
        MessagePartInfo messagePart = getMessagePart(name);
        if (messagePart != null) {
            messageParts.remove(name);
        }
    }
    
    /**
     * Returns the message part with the given name, if found.
     *
     * @param name the qualified name.
     * @return the message part; or <code>null</code> if not found.
     */
    public MessagePartInfo getMessagePart(QName name) {
        return messageParts.get(name);
    }
    
    /**
     * Returns all message parts for this message.
     *
     * @return all message parts.
     */
    public List<MessagePartInfo> getMessageParts() {
        return Collections.unmodifiableList(new ArrayList<MessagePartInfo>(messageParts.values()));
    }
    
    public int size() {
        return messageParts.size();
    }
    

}
