package org.apache.xcf.jibx;

import java.util.Collection;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;

public class JibxDataWriter implements
		DataWriter<XMLStreamWriter> {

	public void write(Object obj, XMLStreamWriter output) {
	}

	public void write(Object obj, MessagePartInfo part, XMLStreamWriter output) {
		try {
			String pfx = output.getPrefix(part.getConcreteName()
					.getNamespaceURI());
			if (StringUtils.isEmpty(pfx)) {
				output.writeStartElement("tns", part.getConcreteName()
						.getLocalPart(), part.getConcreteName()
						.getNamespaceURI());
				output.writeNamespace("tns", part.getConcreteName()
						.getNamespaceURI());
			} else {
				output.writeStartElement(pfx, part.getConcreteName()
						.getLocalPart(), part.getConcreteName()
						.getNamespaceURI());
			}
//			StaxUtils.copy(reader, output, true);
			output.writeCharacters(JibxUtil.toText(obj, part.getTypeQName()));
			output.writeEndElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void setAttachments(Collection<Attachment> attachments) {
	}

	public void setProperty(String key, Object value) {
	}

	public void setSchema(Schema s) {
	}
}
