package org.apache.xcf.jibx;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.ServiceInfo;

public class JiBXDataBinding extends AbstractDataBinding implements
		WrapperCapableDatabinding {

	private static final Logger LOG = LogUtils.getLogger(JiBXDataBinding.class);

	private static final Class<?> SUPPORTED_DATA_READER_FORMATS[] = new Class<?>[] { XMLStreamReader.class };
	private static final Class<?> SUPPORTED_DATA_WRITER_FORMATS[] = new Class<?>[] { XMLStreamWriter.class };

	public WrapperHelper createWrapperHelper(Class<?> wrapperType,
			QName typeName, List<String> partNames, List<String> elTypeNames,
			List<Class<?>> partClasses) {
		throw new UnsupportedOperationException();
	}

	@SuppressWarnings("unchecked")
	public <T> DataReader<T> createReader(Class<T> cls) {
		if (XMLStreamReader.class.equals(cls)) {
			return (DataReader<T>) (new JibxDataReader());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> DataWriter<T> createWriter(Class<T> cls) {
		if (XMLStreamWriter.class.equals(cls)) {
			return (DataWriter<T>) (new JibxDataWriter());
		}
		return null;
	}

	public Class<?>[] getSupportedReaderFormats() {
		return SUPPORTED_DATA_READER_FORMATS;
	}

	public Class<?>[] getSupportedWriterFormats() {
		return SUPPORTED_DATA_WRITER_FORMATS;
	}

	public void initialize(Service service) {
		if (LOG.isLoggable(Level.FINER)) {
			LOG.log(Level.FINER, "Initialize JiBX Databinding for ["
					+ service.getName() + "] service");
		}
		for (ServiceInfo serviceInfo : service.getServiceInfos()) {
			SchemaCollection schemaCollection = serviceInfo
					.getXmlSchemaCollection();
			if (schemaCollection.getXmlSchemas().length > 1) {
				// Schemas are already populated.
				continue;
			}
			JibxXmlSchemaInitializer schemaInit = new JibxXmlSchemaInitializer(
					serviceInfo);
			schemaInit.walk();
		}
	}

}
