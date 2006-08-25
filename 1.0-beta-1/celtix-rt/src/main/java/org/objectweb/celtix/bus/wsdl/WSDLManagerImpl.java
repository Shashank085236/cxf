package org.objectweb.celtix.bus.wsdl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.w3c.dom.Element;

import org.objectweb.celtix.Bus;
import org.objectweb.celtix.BusException;
import org.objectweb.celtix.bus.busimpl.ComponentCreatedEvent;
import org.objectweb.celtix.bus.busimpl.ComponentRemovedEvent;
import org.objectweb.celtix.common.logging.LogUtils;
import org.objectweb.celtix.wsdl.WSDLManager;

/**
 * WSDLManagerImpl
 * 
 * @author dkulp
 */
public class WSDLManagerImpl implements WSDLManager {

    private static final Logger LOG = LogUtils
            .getL7dLogger(WSDLManagerImpl.class);

    final ExtensionRegistry registry;

    final WSDLFactory factory;

    final WeakHashMap<Object, Definition> definitionsMap;
    
    final Bus bus;

    public WSDLManagerImpl(Bus b) throws BusException {
        bus = b;
        try {
            factory = WSDLFactory.newInstance();
            registry = factory.newPopulatedExtensionRegistry();
        } catch (WSDLException e) {
            throw new BusException(e);
        }
        definitionsMap = new WeakHashMap<Object, Definition>();
        // null check for the unit test
        if (bus != null) {
            bus.sendEvent(new ComponentCreatedEvent(this));
        }
    }

    public WSDLFactory getWSDLFactory() {
        return factory;
    }

    /*
     * (non-Javadoc)
     * 
     * XXX - getExtensionRegistry()
     *
     * @see org.objectweb.celtix.wsdl.WSDLManager#getExtenstionRegistry()
     */
    public ExtensionRegistry getExtenstionRegistry() {
        return registry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.celtix.wsdl.WSDLManager#getDefinition(java.net.URL)
     */
    public Definition getDefinition(URL url) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(url)) {
                return definitionsMap.get(url);
            }
        }
        Definition def = loadDefinition(url.toString());
        synchronized (definitionsMap) {
            definitionsMap.put(url, def);
        }
        return def;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.objectweb.celtix.wsdl.WSDLManager#getDefinition(java.lang.String)
     */
    public Definition getDefinition(String url) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(url)) {
                return definitionsMap.get(url);
            }
        }
        return loadDefinition(url);
    }

    public Definition getDefinition(Element el) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(el)) {
                return definitionsMap.get(el);
            }
        }
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setExtensionRegistry(registry);
        Definition def = reader.readWSDL(null, el);
        synchronized (definitionsMap) {
            definitionsMap.put(el, def);
        }
        return def;
    }

    public Definition getDefinition(Class<?> sei) throws WSDLException {
        synchronized (definitionsMap) {
            if (definitionsMap.containsKey(sei)) {
                return definitionsMap.get(sei);
            }
        }

        Definition def = createDefinition(sei);
        synchronized (definitionsMap) {
            definitionsMap.put(sei, def);
        }
        return def;
    }

    private Definition loadDefinition(String url) throws WSDLException {
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        reader.setExtensionRegistry(registry);
        Definition def = reader.readWSDL(url);
        synchronized (definitionsMap) {
            definitionsMap.put(url, def);
        }
        return def;
    }

    private Definition createDefinition(Class<?> sei) {
        Definition definition = null;
        if (LOG.isLoggable(Level.INFO)) {
            LOG.info("createDefinition for class: " + sei.getName());
        }
        File tmp = null;
        try {
            tmp = File.createTempFile("tmp", ".wsdl");
            tmp.delete();
            tmp.mkdir();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "WSDL_GENERATION_TMP_DIR_MSG", ex);
            return null;
        }

        /*
         * JAXWSWsdlGenerator generator = new JAXWSWsdlGenerator(sei.getName(),
         * sei.getClassLoader()); Configuration config = new ToolConfig(new
         * String[] {"-wsdl", "-d", tmp.getPath()});
         * generator.setConfiguration(config); generator.generate();
         */

        try {
            int result = 0;
            org.objectweb.celtix.tools.JavaToWSDL.main(new String[] {"-o",
                    tmp.getPath() + "/tmp.wsdl", sei.getName() });
            if (0 != result) {
                LOG.log(Level.SEVERE, "WSDL_GENERATION_BAD_RESULT_MSG", result);
                return null;
            }

            // schema and WSDL file should have been created in tmp directory

            File[] generated = tmp.listFiles();
            File schema = null;
            File wsdl = null;
            for (File f : generated) {
                if (f.isFile()) {
                    if (null == wsdl && f.getName().endsWith(".wsdl")) {
                        wsdl = f;
                    } else if (null == schema && f.getName().endsWith(".xsd")) {
                        schema = f;
                    }
                    if (null != schema && null != wsdl) {
                        break;
                    }
                }
            }
            if (null == wsdl || null == schema) {
                LOG.severe("WSDL_SCHEMA_GENERATION_FAILURE_MSG");
                return null;
            } else if (LOG.isLoggable(Level.INFO)) {
                LOG.info("Generated " + wsdl.getPath() + " and "
                        + schema.getPath());
            }

            /*
             * WSDLFactory wf = getWSDLFactory();
             * 
             * try { WSDLReader reader = wf.newWSDLReader();
             * reader.setFeature("javax.wsdl.verbose", false);
             * reader.setExtensionRegistry(registry); definition =
             * reader.readWSDL(wsdl.getPath()); } catch (WSDLException ex) {
             * LOG.log(Level.SEVERE, "WSDL_UNREADABLE_MSG", ex); }
             */

            definition = org.objectweb.celtix.tools.JavaToWSDL.getDefinition();

        } finally {
            class Directory {
                private final File dir;

                Directory(File d) {
                    dir = d;
                }

                void delete() {
                    File[] entries = dir.listFiles();
                    for (File f : entries) {
                        if (f.isDirectory()) {
                            Directory d = new Directory(f);
                            d.delete();
                        }
                        f.delete();
                    }
                    dir.delete();
                }
            }
            Directory dir = new Directory(tmp);
            dir.delete();
        }

        return definition;
    }

    public void shutdown() {
        if (bus != null) {
            bus.sendEvent(new ComponentRemovedEvent(this));
        }
    }
}
