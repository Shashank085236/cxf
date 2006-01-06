package org.objectweb.celtix.tools.common;

import javax.xml.namespace.QName;

public final class ToolConstants {

    public static final String TOOLSPECS_BASE = "/org/objectweb/celtix/tools/common/toolspec/toolspecs/";
    public static final String SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";
    public static final String XML_NAMESPACE_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String WSDL_NAMESPACE_URI = "http://schemas.xmlsoap.org/wsdl/";
    
    public static final String DEFAULT_TEMP_DIR = "gen_tmp";
    public static final String CFG_OUTPUTDIR = "outputdir";
    public static final String CFG_WSDLURL = "wsdlurl";
    public static final String CFG_NAMESPACE = "namespace";
    public static final String CFG_VERBOSE = "verbose";
    public static final String CFG_PORT = "port";
    public static final String CFG_BINDING = "binding";
    public static final String CFG_WEBSERVICE = "webservice";
    public static final String CFG_SERVER = "server";
    public static final String CFG_CLIENT = "client";
    public static final String CFG_ALL = "all";
    public static final String CFG_IMPL = "impl";
    public static final String CFG_PACKAGENAME = "packagename";
    public static final String CFG_NINCLUDE = "ninclude";
    public static final String CFG_NEXCLUDE = "nexclude";
    public static final String CFG_CMD_ARG = "args";
    public static final String CFG_INSTALL_DIR = "install.dir";
    public static final String CFG_PLATFORM_VERSION = "platform.version";

    // WSDL2Java Constants
    
    public static final String CFG_TYPES = "types";
    public static final String CFG_INTERFACE = "interface";
    public static final String CFG_NIGNOREEXCLUDE = "nignoreexclude";
    public static final String CFG_ANT = "ant";
    public static final String CFG_LIB_REF = "library.references";
    public static final String CFG_ANT_PROP = "ant.prop";
    
    // Java2WSDL Constants
    
    public static final String CFG_OUTPUTFILE = "outputfile";
    public static final String CFG_TNS = "tns";
    public static final String CFG_SCHEMANS = "schemans";
    public static final String CFG_USETYPES = "usetypes";
    public static final String CFG_CLASSNAME = "classname";
    public static final String CFG_PORTTYPE = "porttype";
 

    // WSDL2Java Processor Constants
    public static final String SEI_GENERATOR = "sei.generator";
    public static final String FAULT_GENERATOR = "fault.generator";
    public static final String TYPE_GENERATOR = "type.generator";
    public static final String IMPL_GENERATOR = "impl.generator";
    public static final String SVR_GENERATOR = "svr.generator";
    public static final String CLT_GENERATOR = "clt.generator";
    public static final String SERVICE_GENERATOR = "service.generator";
    public static final String ANT_GENERATOR = "ant.generator";

    // Binding namespace
    public static final String NS_JAXWS_BINDINGS = "http://java.sun.com/xml/ns/jaxws";
    public static final String NS_JAXB_BINDINGS = "http://java.sun.com/xml/ns/jaxb";
    public static final QName  JAXWS_BINDINGS = new QName(NS_JAXWS_BINDINGS, "bindings");
    public static final String JAXWS_BINDINGS_WSDL_LOCATION = "wsdlLocation";
    public static final String JAXWS_BINDING_NODE = "node";
    public static final String JAXWS_BINDING_VERSION = "version";

    public static final String ASYNC_METHOD_SUFFIX = "Async";
}
