package org.objectweb.celtix.bindings;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.objectweb.celtix.context.ObjectMessageContext;


/**
 * Provides methods for invoking operations on an endpoint.
 * @author dkulp
 *
 */
public interface ClientBinding extends BindingBase {

    /**
     * Makes a one way request using the contents of the <code>ObjectMessageContext</code>.
     * @param context Holds the method and parameters for the request.
     * @param callback Callback object to handle the IO of the objects in the context
     * @throws IOException
     */
    void invokeOneWay(ObjectMessageContext context, DataBindingCallback callback)
        throws IOException;

    /**
     * Makes a synchronous request and returns the context containing the response.
     * @param context Holds the method and parameters for the request.
     * @param callback Callback object to handle the IO of the objects in the context
     * @return ObjectMessageContext containing the request response.
     * @throws IOException
     */
    ObjectMessageContext invoke(ObjectMessageContext context, DataBindingCallback callback)
        throws IOException;

    /**
     * Makes an asynchronous request using the contents of the <code>ObjectMessageContext</code>.
     * @param context Holds the method and parameters for the request.
     * @param callback Callback object to handle the IO of the objects in the context
     * @return Future for accessing the result of the request.
     * @throws IOException
     */
    Future<ObjectMessageContext> invokeAsync(ObjectMessageContext context,
                                       DataBindingCallback callback,
                                       Executor executor)
        throws IOException;
}
