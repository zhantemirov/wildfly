package org.jboss.as.test.multinode.clientinterceptor.transaction;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

public class TransactionInterceptor implements EJBClientInterceptor {

    @Override
    public void handleInvocation(EJBClientInvocationContext context) throws Exception {
        context.sendRequest();
    }

    @Override
    public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
        if (context.getInvokedMethod().getName().equalsIgnoreCase("secondBeanEcho")) {
            throw new RuntimeException("exception from client-side interceptor");
        } else {
            return context.getResult();
        }
    }
}
