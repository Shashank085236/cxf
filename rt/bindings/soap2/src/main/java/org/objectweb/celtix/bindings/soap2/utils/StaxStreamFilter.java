package org.objectweb.celtix.bindings.soap2.utils;

import javax.xml.namespace.QName;
import javax.xml.stream.StreamFilter;
import javax.xml.stream.XMLStreamReader;

public class StaxStreamFilter implements StreamFilter {
    private QName[] tags;

    public StaxStreamFilter(QName[] eventsToReject) {
        tags = eventsToReject;
    }

    public boolean accept(XMLStreamReader reader) {

        if (!reader.isStartElement() && !reader.isEndElement()) {
            return false;
        }

        if (reader.isStartElement()) {
            QName elName = reader.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }

        if (reader.isEndElement()) {
            QName elName = reader.getName();
            for (QName tag : tags) {
                if (elName.equals(tag)) {
                    return false;
                }
            }
        }
        return true;
    }
}
