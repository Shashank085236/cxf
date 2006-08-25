package org.apache.cxf.service.factory;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.helpers.MethodComparator;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.ServiceImpl;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.wsdl11.WSDLServiceFactory;

public class ReflectionServiceFactoryBean extends AbstractServiceFactoryBean {
    private static final Logger LOG = Logger.getLogger(ReflectionServiceFactoryBean.class.getName());
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(ReflectionServiceFactoryBean.class);

    private Class<?> serviceClass;
    private URL wsdlURL;
    private List<AbstractServiceConfiguration> serviceConfigurations = 
        new ArrayList<AbstractServiceConfiguration>();
    private QName serviceName;
    private Invoker invoker;
    private Executor executor;

    public ReflectionServiceFactoryBean() {
        getServiceConfigurations().add(new DefaultServiceConfiguration());
    }

    @Override
    public Service create() {
        initializeServiceConfigurations();

        initializeServiceModel();

        initializeDataBindings();

        initializeDefaultInterceptors();

        getService().setInvoker(getInvoker());
        if (getExecutor() != null) {
            getService().setExecutor(getExecutor());
        } else {
            getService().setExecutor(new Executor() {
                public void execute(Runnable r) {
                    r.run();
                }
            });
        }

        return getService();
    }

    protected void initializeServiceConfigurations() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            c.setServiceFactory(this);
        }
    }

    protected void initializeServiceModel() {
        URL url = getWsdlURL();

        if (url != null) {
            LOG.info("Creating Service " + getServiceQName() + " from WSDL.");
            WSDLServiceFactory factory = new WSDLServiceFactory(getBus(), url, getServiceQName());

            setService(factory.create());

            initializeWSDLOperations();
        } else {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setName(getServiceQName());

            createInterface(serviceInfo);

            ServiceImpl service = new ServiceImpl(serviceInfo);
            setService(service);

            // TODO Add hooks to create default bindings
        }
    }

    protected void initializeWSDLOperations() {
        Method[] methods = serviceClass.getMethods();
        Arrays.sort(methods, new MethodComparator());

        InterfaceInfo intf = getService().getServiceInfo().getInterface();

        for (OperationInfo o : intf.getOperations()) {
            Method selected = null;
            for (Method m : methods) {
                if (isValidMethod(m)) {
                    QName opName = getOperationName(intf, m);

                    if (o.getName().equals(opName)) {
                        selected = m;
                        o.setProperty(Method.class.getName(), m);
                        break;
                    }
                }
            }

            if (selected == null) {
                throw new ServiceConstructionException(new Message("NO_METHOD_FOR_OP", BUNDLE, o.getName()));
            }

            initializeWSDLOperation(intf, o, selected);
        }
    }

    protected void initializeWSDLOperation(InterfaceInfo intf, OperationInfo o, Method selected) {
        // TODO Auto-generated method stub

    }

    protected ServiceInfo createServiceInfo(InterfaceInfo intf) {
        ServiceInfo svcInfo = new ServiceInfo();
        svcInfo.setInterface(intf);

        return svcInfo;
    }

    protected InterfaceInfo createInterface(ServiceInfo serviceInfo) {
        QName intfName = getInterfaceName();
        InterfaceInfo intf = new InterfaceInfo(serviceInfo, intfName);

        Method[] methods = serviceClass.getMethods();

        // The BP profile states we can't have operations of the same name
        // so we have to append numbers to the name. Different JVMs sort methods
        // differently.
        // We need to keep them ordered so if we have overloaded methods, the
        // wsdl is
        // generated the same every time across JVMs and across client/servers.
        Arrays.sort(methods, new MethodComparator());

        for (Method m : serviceClass.getMethods()) {
            if (isValidMethod(m)) {
                createOperation(serviceInfo, intf, m);
            }
        }
        return intf;
    }

    protected OperationInfo createOperation(ServiceInfo serviceInfo, InterfaceInfo intf, Method m) {
        OperationInfo info = intf.addOperation(getOperationName(intf, m));

        createMessageParts(intf, info, m);

        return info;
    }

    protected void createMessageParts(InterfaceInfo intf, OperationInfo op, Method method) {
        //        
        // final Class[] paramClasses = method.getParameterTypes();
        //        
        // // Setup the input message
        // MessageInfo inMsg = op.createMessage(getInputMessageName(op));
        // op.setInput(inMsg.getName().getLocalPart(), inMsg);
        //        
        // for (int j = 0; j < paramClasses.length; j++)
        // {
        // if (!paramClasses[j].equals(MessageContext.class) &&
        // !isHeader(method, j) &&
        // isInParam(method, j))
        // {
        // final QName q = getInParameterName(endpoint, op, method, j, isDoc);
        // MessagePartInfo part = inMsg.addMessagePart(q, paramClasses[j]);
        // part.setIndex(j);
        // part.setSchemaElement(isDoc ||
        // endpoint.getServiceInfo().isWrapped());
        // }
        // }
        //        
        // if (hasOutMessage(mep))
        // {
        // // Setup the output message
        // MessageInfo outMsg = op.createMessage(createOutputMessageName(op));
        // op.setOutput(outMsg.getName().getLocalPart(), outMsg);
        //
        // final Class returnType = method.getReturnType();
        // if (!returnType.isAssignableFrom(void.class) && !isHeader(method,
        // -1))
        // {
        // final QName q = getOutParameterName(endpoint, op, method, -1, isDoc);
        // MessagePartInfo part = outMsg.addMessagePart(q,
        // method.getReturnType());
        // }
        //            
        // for (int j = 0; j < paramClasses.length; j++)
        // {
        // if (!paramClasses[j].equals(MessageContext.class) &&
        // !isHeader(method, j) &&
        // isOutParam(method, j))
        // {
        // final QName q = getInParameterName(endpoint, op, method, j, isDoc);
        // MessagePartInfo part = outMsg.addMessagePart(q, paramClasses[j]);
        // part.setIndex(j);
        // part.setSchemaElement(isDoc ||
        // endpoint.getServiceInfo().isWrapped());
        // }
        // }
        // }
        //
        // initializeFaults(intf, op, method);
    }

    protected QName getServiceQName() {
        if (serviceName == null) {
            serviceName = new QName(getServiceNamespace(), getServiceName());
        }

        return serviceName;
    }

    protected QName getInterfaceName() {
        // TODO Auto-generated method stub
        return null;
    }

    protected String getServiceName() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String name = c.getServiceName();
            if (name != null) {
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected String getServiceNamespace() {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            String name = c.getServiceNamespace();
            if (name != null) {
                return name;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean isValidMethod(final Method method) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Boolean b = c.isOperation(method);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected boolean isOutParam(Method method, int j) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            Boolean b = c.isOutParam(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected boolean isInParam(Method method, int j) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            Boolean b = c.isInParam(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected QName getInputMessageName(final OperationInfo op) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName q = c.getInputMessageName(op);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName createOutputMessageName(final OperationInfo op) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName q = c.getOutputMessageName(op);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean hasOutMessage(String mep) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            Boolean b = c.hasOutMessage(mep);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected void initializeFaults(final InterfaceInfo service, 
                                    final OperationInfo op, 
                                    final Method method) {
        // Set up the fault messages
        final Class[] exceptionClasses = method.getExceptionTypes();
        for (int i = 0; i < exceptionClasses.length; i++) {
            Class exClazz = exceptionClasses[i];

            // Ignore XFireFaults because they don't need to be declared
            if (exClazz.equals(Exception.class) || exClazz.equals(RuntimeException.class)
                || exClazz.equals(Throwable.class)) {
                continue;
            }

            addFault(service, op, exClazz);
        }
    }

    protected FaultInfo addFault(final InterfaceInfo service, final OperationInfo op, Class exClass) {
        // TODO
        return null;
    }

    protected QName getFaultName(Service service, OperationInfo o, Class exClass, Class beanClass) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName q = c.getFaultName(service, o, exClass, beanClass);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected String getAction(OperationInfo op) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            String s = c.getAction(op);
            if (s != null) {
                return s;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean isHeader(Method method, int j) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            Boolean b = c.isHeader(method, j);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    /**
     * Creates a name for the operation from the method name. If an operation
     * with that name already exists, a name is create by appending an integer
     * to the end. I.e. if there is already two methods named
     * <code>doSomething</code>, the first one will have an operation name of
     * "doSomething" and the second "doSomething1".
     * 
     * @param service
     * @param method
     */
    protected QName getOperationName(InterfaceInfo service, Method method) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName s = c.getOperationName(service, method);
            if (s != null) {
                return s;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected boolean isAsync(final Method method) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            Boolean b = c.isAsync(method);
            if (b != null) {
                return b.booleanValue();
            }
        }
        return true;
    }

    protected QName getInParameterName(final Service service, final OperationInfo op, final Method method,
                                       final int paramNumber, final boolean doc) {
        if (paramNumber == -1) {
            throw new RuntimeException();
        }
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName q = c.getInParameterName(op, method, paramNumber, doc);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }

    protected QName getOutParameterName(final Service service, final OperationInfo op, final Method method,
                                        final int paramNumber, final boolean doc) {
        for (Iterator itr = serviceConfigurations.iterator(); itr.hasNext();) {
            AbstractServiceConfiguration c = (AbstractServiceConfiguration)itr.next();
            QName q = c.getOutParameterName(op, method, paramNumber, doc);
            if (q != null) {
                return q;
            }
        }
        throw new IllegalStateException("ServiceConfiguration must provide a value!");
    }


    protected Class getResponseWrapper(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Class cls = c.getResponseWrapper(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }
    protected Class getRequestWrapper(Method selected) {
        for (AbstractServiceConfiguration c : serviceConfigurations) {
            Class cls = c.getRequestWrapper(selected);
            if (cls != null) {
                return cls;
            }
        }
        return null;
    }

    
    
    public List<AbstractServiceConfiguration> getConfigurations() {
        return serviceConfigurations;
    }

    public void setConfigurations(List<AbstractServiceConfiguration> configurations) {
        this.serviceConfigurations = configurations;
    }

    public Class<?> getServiceClass() {
        return serviceClass;
    }

    public void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public URL getWsdlURL() {
        if (wsdlURL == null) {
            for (AbstractServiceConfiguration c : serviceConfigurations) {
                wsdlURL = c.getWsdlURL();
                if (wsdlURL != null) {
                    break;
                }
            }
        }
        return wsdlURL;
    }

    public void setWsdlURL(URL wsdlURL) {
        this.wsdlURL = wsdlURL;
    }

    public List<AbstractServiceConfiguration> getServiceConfigurations() {
        return serviceConfigurations;
    }

    public void setServiceConfigurations(List<AbstractServiceConfiguration> serviceConfigurations) {
        this.serviceConfigurations = serviceConfigurations;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public Invoker getInvoker() {
        return invoker;
    }

    public void setInvoker(Invoker invoker) {
        this.invoker = invoker;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }
}
