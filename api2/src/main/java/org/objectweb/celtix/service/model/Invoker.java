package org.objectweb.celtix.service.model;

public interface Invoker {
    //REVISIT - what is needed for the invoker?
    //Actually, is this even needed?  Should the frontend just provide a "final" interceptor instead?
    Object invoke(Object o);
}
