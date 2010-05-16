package org.apache.xcf.jibx;

import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.service.ServiceModelVisitor;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.ws.commons.schema.XmlSchemaElement;

public class JibxXmlSchemaInitializer extends ServiceModelVisitor {
	private static final Logger LOG = LogUtils.getLogger(JibxXmlSchemaInitializer.class);
	
	private SchemaCollection schemas;

	public JibxXmlSchemaInitializer(ServiceInfo serviceInfo) {
		super(serviceInfo);
		schemas = serviceInfo.getXmlSchemaCollection();
	}

	@Override
	public void begin(MessagePartInfo part) {
		LOG.finest(part.getName().toString());
		// Check to see if the WSDL information has been filled in for us.
		if (part.getTypeQName() != null || part.getElementQName() != null) {
			checkForExistence(part);
			return;
		}

		Class<?> clazz = part.getTypeClass();
		if (clazz == null) {
			return;
		}

		boolean isFromWrapper = part.getMessageInfo().getOperation()
				.isUnwrapped();
		if (isFromWrapper && clazz.isArray()
				&& !Byte.TYPE.equals(clazz.getComponentType())) {
			clazz = clazz.getComponentType();
		}
		
		mapClass(part, clazz);
	}
	
	 public void checkForExistence(MessagePartInfo part) {
	        QName qn = part.getElementQName();
	        if (qn != null) {
	            XmlSchemaElement el = schemas.getElementByQName(qn);
	            if (el == null) {
	                Class<?> clazz = part.getTypeClass();
	                if (clazz == null) {
	                    return;
	                }

	                boolean isFromWrapper = part.getMessageInfo().getOperation().isUnwrapped();
	                if (isFromWrapper && clazz.isArray() && !Byte.TYPE.equals(clazz.getComponentType())) {
	                    clazz = clazz.getComponentType();
	                }
	                mapClass(part, clazz);
	            }
	        }
	    }
	 
	 private void mapClass(MessagePartInfo part, Class<?> clazz) {
		 String clazzName = clazz.getName();
		 if (Types.isSimpleValue(clazzName)) {
			 QName schemaType = Types.getSchemaType(clazzName);
			 part.setTypeQName(schemaType);
			 part.setXmlSchema(schemas.getTypeByQName(schemaType));
			 
		 }		 
	 }

}
