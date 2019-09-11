package org.jboss.as.test.multinode.clientinterceptor.transaction;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
@Remote(FirstBeanRemote.class)
public class FirstBean implements FirstBeanRemote {

    @EJB
    SecondBeanRemote secondBean;

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    @Override
    public String callSecondBeanMethod(String msg) throws RuntimeException {
        secondBean.clear();
        secondBean.enlistResource();
        return secondBean.secondBeanEcho(msg);
    }

    @Override
    public boolean areResourcesRollbacked() {
        return secondBean.areResourcesRollbacked();
    }
}
