package org.objectweb.celtix.bindings;

import org.objectweb.celtix.context.InputStreamMessageContext;
import org.objectweb.celtix.transports.ServerTransport;

public class ServerBindingCallback implements Runnable {

    private final InputStreamMessageContext context;
    private final ServerTransport transport;
    private final AbstractServerBinding binding;

    public ServerBindingCallback(InputStreamMessageContext ctx, ServerTransport t, AbstractServerBinding b) {
        context = ctx;
        transport = t;
        binding = b;
    }

    public void run() {
        binding.dispatch(context, transport);
    }

}
