package org.apache.cxf.configuration.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.configuration.ConfigurationItemMetadata;
import org.apache.cxf.configuration.ConfigurationMetadata;

public class ConfigurationMetadataImpl implements ConfigurationMetadata {

    private final Map<String, ConfigurationItemMetadata> definitions;
    private String namespaceURI;
    private String parentNamespaceURI;

    public ConfigurationMetadataImpl() {
        definitions = new HashMap<String, ConfigurationItemMetadata>();
    }

    protected void addItem(ConfigurationItemMetadata item) {
        definitions.put(item.getName(), item);
    }

    public String getNamespaceURI() {
        return namespaceURI;
    }

    public String getParentNamespaceURI() {
        return parentNamespaceURI;
    }

    public ConfigurationItemMetadata getDefinition(String name) {
        return definitions.get(name);
    }

    public Collection<ConfigurationItemMetadata> getDefinitions() {
        return definitions.values();
    }

    protected void setNamespaceURI(String uri) {
        namespaceURI = uri;
    }

    protected void setParentNamespaceURI(String uri) {
        parentNamespaceURI = uri;
    }
}
