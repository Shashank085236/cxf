package org.objectweb.celtix.message;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.objectweb.celtix.interceptors.InterceptorChain;
import org.objectweb.celtix.messaging.Conduit;
import org.objectweb.celtix.messaging.Destination;

/**
 * A base class to build your own message implementations on.
 * 
 * @author Dan
 */
public abstract class AbstractWrappedMessage implements Message {
    
    private Message message;

    protected AbstractWrappedMessage(Message msg) {
        this.message = msg;
    }

    public void clear() {
        message.clear();
    }

    public boolean containsKey(Object key) {
        return message.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return message.containsValue(value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return message.entrySet();
    }

    public boolean equals(Object o) {
        return message.equals(o);
    }

    public Object get(Object key) {
        return message.get(key);
    }

    public Collection<Attachment> getAttachments() {
        return message.getAttachments();
    }

    public String getAttachmentMimeType() {
        return message.getAttachmentMimeType();
    }
    
    public Conduit getConduit() {
        return message.getConduit();
    }
    
    public Destination getDestination() {
        return message.getDestination();
    }

    public Exchange getExchange() {
        return message.getExchange();
    }

    public String getId() {
        return message.getId();
    }

    public InterceptorChain getInterceptorChain() {
        return message.getInterceptorChain();
    }

    public <T> T getContent(Class<T> format) {
        return message.getContent(format);
    }

    public Set<Class> getContentFormats() {
        return message.getContentFormats();
    }

    public int hashCode() {
        return message.hashCode();
    }

    public boolean isEmpty() {
        return message.isEmpty();
    }

    public Set<String> keySet() {
        return message.keySet();
    }

    public Object put(String key, Object value) {
        return message.put(key, value);
    }

    public void putAll(Map<? extends String, ? extends Object> t) {
        message.putAll(t);
    }

    public Object remove(Object key) {
        return message.remove(key);
    }

    public <T> void setContent(Class<T> format, Object content) {
        message.setContent(format, content);
    }

    public int size() {
        return message.size();
    }

    public Collection<Object> values() {
        return message.values();
    }
    
}
