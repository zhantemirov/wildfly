package org.jboss.as.test.multinode.clientinterceptor.transaction;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.transaction.TransactionManager;

@Stateless
@Remote(SecondBeanRemote.class)
public class SecondBean implements SecondBeanRemote {

    private List<MyXAResource> enlistedResources = new ArrayList<>();

    @Resource(lookup = "java:/TransactionManager")
    TransactionManager txm;

    @Override
    public void clear() {
        enlistedResources = new ArrayList<>();
    }

    @Override
    public void enlistResource() {
        MyXAResource xar = new MyXAResource();
        try {
            txm.getTransaction().enlistResource(xar);
            enlistedResources.add(xar);
        } catch (Exception e) {
            //
        }
    }

    @Override
    public boolean areResourcesRollbacked() {
        for (MyXAResource resource : enlistedResources) {
            if (!resource.getPhases().contains(MyXAResource.State.ROLLBACK)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String secondBeanEcho(String echoMsh) throws RuntimeException {
        return echoMsh;
    }

}
