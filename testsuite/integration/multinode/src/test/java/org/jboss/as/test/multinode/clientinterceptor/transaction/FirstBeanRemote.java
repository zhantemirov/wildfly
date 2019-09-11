package org.jboss.as.test.multinode.clientinterceptor.transaction;

public interface FirstBeanRemote {

    String callSecondBeanMethod(String msg);

    boolean areResourcesRollbacked();
}
