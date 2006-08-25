package org.apache.cxf.binding;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingOperation;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.service.model.AbstractPropertiesHolder;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.WSDLBindingFactory;

import static org.apache.cxf.helpers.CastUtils.cast;

public abstract class AbstractBindingFactory implements BindingFactory, WSDLBindingFactory {

    @Resource
    Bus bus;
    
    @Resource
    Collection<String> activationNamespaces;
    
    @PostConstruct
    void registerWithBindingManager() {
        BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
        for (String ns : activationNamespaces) {
            manager.registerBindingFactory(ns, this);
        }
    }
    
    /**
     * Creates a "default" BindingInfo object for the service.  Called by 
     * createBindingInfo(ServiceInfo service, Binding binding) to actually 
     * create the BindingInfo.  Can return a subclass which can then process
     * the extensors within the subclass.
     * @param service
     * @return
     */
    public BindingInfo createBindingInfo(ServiceInfo service, String namespace) {
        return new BindingInfo(service, namespace);
    }
    
    
    /**
     * Copies extensors from the Binding to BindingInfo.
     * @param service
     * @param binding
     * @return
     */
    public BindingInfo createBindingInfo(ServiceInfo service, Binding binding) {

        String namespace = ((ExtensibilityElement)binding.getExtensibilityElements().get(0))
            .getElementType().getNamespaceURI();
        BindingInfo bi = createBindingInfo(service, namespace);
        
        return initializeBindingInfo(service, binding, bi);
    }

    protected BindingInfo initializeBindingInfo(ServiceInfo service, Binding binding, BindingInfo bi) {
        bi.setName(binding.getQName());
        copyExtensors(bi, binding.getExtensibilityElements());

        for (BindingOperation bop : cast(binding.getBindingOperations(), BindingOperation.class)) {
            String inName = null;
            String outName = null;
            if (bop.getBindingInput() != null) {
                inName = bop.getBindingInput().getName();
            }
            if (bop.getBindingOutput() != null) {
                outName = bop.getBindingOutput().getName();
            }
            BindingOperationInfo bop2 = bi.buildOperation(new QName(service.getName().getNamespaceURI(),
                                                                    bop.getName()), inName, outName);
            if (bop2 != null) {

                copyExtensors(bop2, bop.getExtensibilityElements());
                bi.addOperation(bop2);
                if (bop.getBindingInput() != null) {
                    copyExtensors(bop2.getInput(), bop.getBindingInput().getExtensibilityElements());
                }
                if (bop.getBindingOutput() != null) {
                    copyExtensors(bop2.getOutput(), bop.getBindingOutput().getExtensibilityElements());
                }
                for (BindingFault f : cast(bop.getBindingFaults().values(), BindingFault.class)) {
                    copyExtensors(bop2.getFault(f.getName()), bop.getBindingFault(f.getName())
                        .getExtensibilityElements());
                }
            }
        }
        return bi;
    }

    private void copyExtensors(AbstractPropertiesHolder info, List<?> extList) {
        if (info != null) {
            for (ExtensibilityElement ext : cast(extList, ExtensibilityElement.class)) {
                info.addExtensor(ext);
            }
        }
    }
}
