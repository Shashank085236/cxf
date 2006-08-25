package org.objectweb.celtix.tools.common.model;

import java.util.*;
import javax.jws.soap.SOAPBinding;
import org.w3c.dom.Element;
import org.objectweb.celtix.tools.common.ToolException;
import org.objectweb.celtix.tools.extensions.jaxws.JAXWSBinding;

public class JavaInterface {

    private String name;
    private String packageName;
    private String namespace;
    private String location;
    private JavaModel model;
    private SOAPBinding.Style soapStyle;
    private SOAPBinding.Use soapUse;
    private SOAPBinding.ParameterStyle soapParameterStyle;
    
    private final List<JavaMethod> methods = new ArrayList<JavaMethod>();
    private final List<String> annotations = new ArrayList<String>();
    private final Set<String> imports = new TreeSet<String>();

    private JAXWSBinding jaxwsBinding = new JAXWSBinding();
    private JAXWSBinding bindingExt = new JAXWSBinding();
    
    private String webserviceName;
    private Element handlerChains;
    
    public JavaInterface() {
    }
    
    public JavaInterface(JavaModel m) {
        this.model = m;
    }

    public void setWebServiceName(String wsn) {
        this.webserviceName = wsn;
    }

    public String getWebServiceName() {
        return this.webserviceName;
    }

    public void setSOAPStyle(SOAPBinding.Style s) {
        this.soapStyle = s;
    }

    public SOAPBinding.Style getSOAPStyle() {
        return this.soapStyle;
    }

    public void setSOAPUse(SOAPBinding.Use u) {
        this.soapUse = u;
    }

    public SOAPBinding.Use getSOAPUse() {
        return this.soapUse;
    }

    public void setSOAPParameterStyle(SOAPBinding.ParameterStyle p) {
        this.soapParameterStyle = p;
    }    
    
    public SOAPBinding.ParameterStyle getSOAPParameterStyle() {
        return this.soapParameterStyle;
    }
    
    public JavaModel getJavaModel() {
        return this.model;
    }
    
    public void setName(String n) {
        this.name = n;
    }
    
    public String getName() {
        return name;
    }

    public void setLocation(String l) {
        this.location = l;
    }

    public String getLocation() {
        return this.location;
    }

    public List<JavaMethod> getMethods() {
        return methods;
    }

    public boolean hasMethod(JavaMethod method) {
        if (method != null) {
            String signature = method.getSignature();
            for (int i = 0; i < methods.size(); i++) {
                if (signature.equals(methods.get(i).getSignature())) {
                    return true;
                }
            }
        }
        return false;
    }

    public int indexOf(JavaMethod method) {
        if (method != null) {
            String signature = method.getSignature();
            for (int i = 0; i < methods.size(); i++) {
                if (signature.equals(methods.get(i).getSignature())) {
                    return i;
                }
            }
        }
        return -1;
    }

    public int removeMethod(JavaMethod method) {
        int index = indexOf(method);
        if (index > -1) {
            methods.remove(index);
        }
        return index;
    }

    public void replaceMethod(JavaMethod method) {
        int index = removeMethod(method);
        methods.add(index, method);
    }

    public void addMethod(JavaMethod method) throws ToolException {
        if (hasMethod(method)) {
            replaceMethod(method);
        } else {
            methods.add(method);
        }
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String pn) {
        this.packageName = pn;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public void setNamespace(String ns) {
        this.namespace = ns;
    }

    public void addAnnotation(String annotation) {
        this.annotations.add(annotation);
    }

    public List getAnnotations() {
        return this.annotations;
    }

    public JAXWSBinding getJAXWSBinding() {
        return this.jaxwsBinding;
    }
    
    public void setJAXWSBinding(JAXWSBinding binding) {
        if (binding != null) {
            this.jaxwsBinding = binding;
        }
    }

    public void addImport(String i) {
        imports.add(i);
    }

    public Iterator<String> getImports() {
        return imports.iterator();
    }

    public Element getHandlerChains() {
        return this.handlerChains;
    }

    public void setHandlerChains(Element elem) {
        this.handlerChains = elem;
    }

    public JAXWSBinding getBindingExt() {
        return bindingExt;
    }

    public void setBindingExt(JAXWSBinding pBindingExt) {
        this.bindingExt = pBindingExt;
    }
}
